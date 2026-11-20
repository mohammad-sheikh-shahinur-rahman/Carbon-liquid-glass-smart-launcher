package com.isg32.carino.platform

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterViewAnimator
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView
import com.isg32.carino.data.WidgetProviderInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android AppWidgetHost coordinator for the Widgets tab.
 */
class CarinoWidgets(private val activity: Activity) {

    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)
    val host = AppWidgetHost(activity.applicationContext, HOST_ID)
    private val launcherApps = activity.getSystemService(LauncherApps::class.java)
    private val users = activity.getSystemService(UserManager::class.java)

    private var pendingCallback: ((Int?) -> Unit)? = null
    private var pendingId: Int = -1
    private val views = mutableMapOf<Int, View>()

    fun onResume() {
        runCatching { host.startListening() }
    }

    fun onPause() {
        runCatching { host.stopListening() }
    }

    suspend fun providers(): List<WidgetProviderInfo> = withContext(Dispatchers.IO) {
        val out = mutableListOf<WidgetProviderInfo>()
        for (profile in launcherApps?.profiles.orEmpty()) {
            for (info in manager.getInstalledProvidersForProfile(profile)) {
                out.add(describe(info))
            }
        }
        out.sortedWith(compareBy({ it.app.lowercase() }, { it.label.lowercase() }))
    }

    suspend fun preview(provider: String, user: Long): Bitmap? = withContext(Dispatchers.IO) {
        val info = find(provider, user) ?: return@withContext null
        val d = info.loadPreviewImage(activity, activity.resources.displayMetrics.densityDpi)
            ?: info.loadIcon(activity, activity.resources.displayMetrics.densityDpi)
            ?: return@withContext null
        val w = d.intrinsicWidth.coerceIn(1, 600)
        val h = (d.intrinsicHeight * w / d.intrinsicWidth.coerceAtLeast(1)).coerceIn(1, 600)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, w, h)
        d.draw(Canvas(bmp))
        bmp
    }

    fun info(id: Int): WidgetProviderInfo? =
        manager.getAppWidgetInfo(id)?.let { describe(it) }

    fun add(provider: WidgetProviderInfo, onResult: (Int?) -> Unit) {
        val info = find(provider.provider, provider.user)
        if (info == null) {
            onResult(null)
            return
        }
        val id = host.allocateAppWidgetId()
        if (manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)) {
            configureOrFinish(id, onResult)
        } else {
            pendingCallback?.invoke(null)
            pendingCallback = onResult
            pendingId = id
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            runCatching {
                activity.startActivityForResult(intent, REQUEST_BIND)
            }.onFailure {
                pendingCallback = null
                host.deleteAppWidgetId(id)
                onResult(null)
            }
        }
    }

    fun configure(id: Int) {
        runCatching {
            host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_RECONFIGURE, null)
        }
    }

    fun remove(id: Int) {
        views.remove(id)
        runCatching { host.deleteAppWidgetId(id) }
    }

    fun createHostView(id: Int, widthDp: Float, heightDp: Float): View {
        val info = manager.getAppWidgetInfo(id)
        val view: View = if (info == null) {
            TextView(activity).apply { text = "This widget is no longer available" }
        } else {
            host.createView(activity, id, info).apply {
                updateWidgetBounds(this, widthDp, heightDp)
            }
        }
        views[id] = view
        return view
    }

    fun updateWidgetBounds(view: View, widthDp: Float, heightDp: Float) {
        if (view is AppWidgetHostView) {
            if (Build.VERSION.SDK_INT >= 31) {
                view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
            } else {
                @Suppress("DEPRECATION")
                view.updateAppWidgetSize(null, widthDp.toInt(), heightDp.toInt(), widthDp.toInt(), heightDp.toInt())
            }
        }
    }

    fun isScrollable(id: Int): Boolean {
        val view = views[id] ?: return false
        return scrolls(view)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        val callback = pendingCallback
        when (requestCode) {
            REQUEST_BIND -> {
                if (callback == null) return
                pendingCallback = null
                if (resultCode == Activity.RESULT_OK) {
                    configureOrFinish(pendingId, callback)
                } else {
                    host.deleteAppWidgetId(pendingId)
                    callback(null)
                }
            }
            REQUEST_CONFIGURE -> {
                if (callback == null) return
                pendingCallback = null
                if (resultCode == Activity.RESULT_OK) {
                    callback(pendingId)
                } else {
                    host.deleteAppWidgetId(pendingId)
                    callback(null)
                }
            }
        }
    }

    private fun configureOrFinish(id: Int, onResult: (Int?) -> Unit) {
        val info = manager.getAppWidgetInfo(id)
        if (info?.configure == null) {
            onResult(id)
            return
        }
        pendingCallback = onResult
        pendingId = id
        try {
            host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null)
        } catch (_: Exception) {
            pendingCallback = null
            onResult(id)
        }
    }

    private fun scrolls(view: View): Boolean {
        if (view is AbsListView ||
            view is AdapterViewAnimator ||
            view is ScrollView ||
            view is HorizontalScrollView
        ) {
            return true
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (scrolls(view.getChildAt(i))) return true
            }
        }
        return false
    }

    private fun find(provider: String, user: Long): AppWidgetProviderInfo? {
        val component = ComponentName.unflattenFromString(provider) ?: return null
        val profile = users?.getUserForSerialNumber(user) ?: return null
        return manager.getInstalledProvidersForProfile(profile).firstOrNull { it.provider == component }
    }

    private fun describe(info: AppWidgetProviderInfo): WidgetProviderInfo {
        val pm = activity.packageManager
        val app = try {
            pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
        } catch (_: Exception) {
            info.provider.packageName
        }
        val density = activity.resources.displayMetrics.density
        return WidgetProviderInfo(
            provider = info.provider.flattenToString(),
            user = users?.getSerialNumberForUser(info.profile) ?: 0L,
            pkg = info.provider.packageName,
            label = info.loadLabel(pm),
            app = app,
            minWidth = (info.minWidth / density).toInt(),
            minHeight = (info.minHeight / density).toInt(),
            configurable = info.configure != null,
        )
    }

    companion object {
        const val HOST_ID = 2026
        const val REQUEST_BIND = 4201
        const val REQUEST_CONFIGURE = 4202
        const val REQUEST_RECONFIGURE = 4203
    }
}
