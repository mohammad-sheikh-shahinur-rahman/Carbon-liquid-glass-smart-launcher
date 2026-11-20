package com.isg32.carino.platform

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import com.isg32.carino.R
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.AppShortcut
import com.isg32.carino.data.BatteryState
import com.isg32.carino.data.CalendarEvent
import com.isg32.carino.data.ContactHit
import com.isg32.carino.data.HomeLayout
import com.isg32.carino.data.MediaState
import com.isg32.carino.data.UpNext
import com.isg32.carino.data.UpNextEvent
import com.isg32.carino.data.WeatherPlace
import com.isg32.carino.ui.theme.VWallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Everything Cariño asks of Android as a home app: app listing, disk-cached icons,
 * launching with custom transitions, shortcuts, default-home role, wallpapers,
 * notification shade, torch, media sessions, calendar, contacts, and widgets.
 */
class LauncherRepository(private val activity: Activity) {

    private val main = Handler(Looper.getMainLooper())
    private val launcherApps = activity.getSystemService(LauncherApps::class.java)
    private val userManager = activity.getSystemService(UserManager::class.java)
    private val camera = activity.getSystemService(CameraManager::class.java)

    val widgets = CarinoWidgets(activity)

    private val _battery = MutableStateFlow<BatteryState?>(null)
    val battery: StateFlow<BatteryState?> = _battery.asStateFlow()

    private val _torchOn = MutableStateFlow(false)
    val torchOn: StateFlow<Boolean> = _torchOn.asStateFlow()

    private val _dots = MutableStateFlow<Map<String, Int>>(CarinoNotifications.counts)
    val dots: StateFlow<Map<String, Int>> = _dots.asStateFlow()

    private val _media = MutableStateFlow<MediaState?>(null)
    val media: StateFlow<MediaState?> = _media.asStateFlow()

    private val _packagesChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val packagesChanged = _packagesChanged.asSharedFlow()

    private val _homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val homePressed = _homePressed.asSharedFlow()

    private val _resumed = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val resumed = _resumed.asSharedFlow()

    private var resumedFlag = false
    private var batteryRegistered = false

    private val mediaTracker = CarinoMedia(activity) { _media.value = it }

    var onPickImageRequest: (((String?) -> Unit) -> Unit)? = null
    var onRequestPermission: ((String, (Boolean) -> Unit) -> Unit)? = null
    var onRequestHomeRole: ((Intent) -> Unit)? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            if (level >= 0 && scale > 0) {
                _battery.value = BatteryState(level * 100 / scale, charging)
            }
        }
    }

    private val packageCallback = object : LauncherApps.Callback() {
        private fun changed() {
            _packagesChanged.tryEmit(Unit)
        }
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = changed()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = changed()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = changed()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = changed()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = changed()
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            _torchOn.value = enabled
        }
    }

    init {
        fitWallpaperToScreen()
        CarinoNotifications.onChange = { counts ->
            main.post { _dots.value = counts }
        }
        launcherApps?.registerCallback(packageCallback, main)
        runCatching { camera?.registerTorchCallback(torchCallback, main) }
        readBatteryNow()
    }

    fun onHomePressed() {
        _homePressed.tryEmit(Unit)
    }

    fun onResume() {
        resumedFlag = true
        startBattery()
        mediaTracker.start()
        widgets.onResume()
        _dots.value = CarinoNotifications.counts
        _resumed.tryEmit(Unit)
    }

    fun onPause() {
        resumedFlag = false
        stopBattery()
        mediaTracker.stop()
        widgets.onPause()
    }

    fun dispose() {
        CarinoNotifications.onChange = null
        mediaTracker.stop()
        runCatching { launcherApps?.unregisterCallback(packageCallback) }
        runCatching { camera?.unregisterTorchCallback(torchCallback) }
        stopBattery()
    }

    private fun readBatteryNow() {
        val intent = activity.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        if (level >= 0 && scale > 0) {
            _battery.value = BatteryState(
                level * 100 / scale,
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            )
        }
    }

    private fun startBattery() {
        if (!resumedFlag || batteryRegistered) return
        batteryRegistered = true
        activity.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.let { batteryReceiver.onReceive(activity, it) }
    }

    private fun stopBattery() {
        if (!batteryRegistered) return
        batteryRegistered = false
        runCatching { activity.unregisterReceiver(batteryReceiver) }
    }

    // region Apps

    private fun keyOf(info: LauncherActivityInfo): String =
        "${info.componentName.flattenToString()}#${userManager?.getSerialNumberForUser(info.user) ?: 0L}"

    private fun findActivity(key: String): LauncherActivityInfo? {
        val hash = key.lastIndexOf('#')
        if (hash < 0) return null
        val component = ComponentName.unflattenFromString(key.substring(0, hash)) ?: return null
        val serial = key.substring(hash + 1).toLongOrNull() ?: return null
        val user = userManager?.getUserForSerialNumber(serial) ?: return null
        return launcherApps?.getActivityList(component.packageName, user)
            ?.firstOrNull { it.componentName == component }
    }

    suspend fun listApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val me = Process.myUserHandle()
        val out = mutableListOf<AppEntry>()
        for (user in launcherApps?.profiles.orEmpty()) {
            val work = user != me
            val paused = work && runCatching { userManager?.isQuietModeEnabled(user) ?: false }.getOrDefault(false)
            for (info in launcherApps?.getActivityList(null, user).orEmpty()) {
                if (info.componentName.packageName == activity.packageName) continue
                val installed = try {
                    activity.packageManager.getPackageInfo(info.componentName.packageName, 0).firstInstallTime
                } catch (_: PackageManager.NameNotFoundException) {
                    0L
                }
                val pkgName = info.componentName.packageName
                val appLabel = info.label.toString()
                val category = classifyApp(
                    androidCategory = info.applicationInfo.category,
                    pkg = pkgName,
                    label = appLabel,
                )
                out.add(
                    AppEntry(
                        key = keyOf(info),
                        pkg = pkgName,
                        label = appLabel,
                        installedAt = installed,
                        work = work,
                        paused = paused,
                        category = category,
                    ),
                )
            }
        }
        out.sortedBy { it.label.lowercase() }
    }

    private fun classifyApp(androidCategory: Int, pkg: String, label: String): String {
        val lowerPkg = pkg.lowercase()
        val lowerLabel = label.lowercase()
        val combined = "$lowerPkg $lowerLabel"

        if (listOf("bkash", "nagad", "rocket", "bank", "wallet", "pay", "upay", "cash", "finance", "credit")
                .any { combined.contains(it) }
        ) {
            return "Finance"
        }

        if (androidCategory == android.content.pm.ApplicationInfo.CATEGORY_SOCIAL ||
            listOf(
                "whatsapp", "messenger", "telegram", "signal", "discord", "facebook", "lite",
                "messaging", "mms", "sms", "dialer", "phone", "contacts", "mail", "gmail",
                "chat", "brilliant", "viber", "imo", "skype", "slack", "teams", "zoom",
            ).any { combined.contains(it) }
        ) {
            return "Communication"
        }

        if (androidCategory == android.content.pm.ApplicationInfo.CATEGORY_GAME ||
            androidCategory == android.content.pm.ApplicationInfo.CATEGORY_VIDEO ||
            androidCategory == android.content.pm.ApplicationInfo.CATEGORY_AUDIO ||
            androidCategory == android.content.pm.ApplicationInfo.CATEGORY_IMAGE ||
            listOf(
                "youtube", "netflix", "spotify", "tiktok", "music", "gallery", "photos",
                "camera", "video", "player", "retouch", "editor", "game", "cinema", "tv",
                "podcast", "instagram", "snapchat", "reddit", "pinterest",
            ).any { combined.contains(it) }
        ) {
            return "Entertainment"
        }

        if (androidCategory == android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY ||
            androidCategory == android.content.pm.ApplicationInfo.CATEGORY_MAPS ||
            androidCategory == android.content.pm.ApplicationInfo.CATEGORY_ACCESSIBILITY ||
            listOf(
                "calculator", "calendar", "clock", "compass", "chrome", "browser", "files",
                "documents", "drive", "settings", "authenticator", "assistant", "keyboard",
                "type", "maps", "translate", "notes", "keep", "recorder", "weather", "gpt",
                "chatgpt", "care", "tool", "utility", "scanner", "pdf", "vpn", "store", "vending",
            ).any { combined.contains(it) }
        ) {
            return "Tools"
        }

        return "Other"
    }

    suspend fun loadIcon(key: String, size: Int = 160, mono: Boolean = false): Bitmap? =
        withContext(Dispatchers.IO) {
            val info = findActivity(key) ?: return@withContext null
            val updated = try {
                activity.packageManager.getPackageInfo(info.componentName.packageName, 0).lastUpdateTime
            } catch (_: PackageManager.NameNotFoundException) {
                0L
            }
            val dir = File(activity.cacheDir, "icons").apply { mkdirs() }
            val name = "${Integer.toHexString(key.hashCode())}-$size-${if (mono) "m" else "c"}-$updated"
            val png = File(dir, "$name.png")
            val none = File(dir, "$name.none")
            if (png.exists()) {
                return@withContext BitmapFactory.decodeFile(png.absolutePath)
            }
            if (none.exists()) return@withContext null
            val bmp = renderIcon(info, size, mono)
            runCatching {
                if (bmp != null) {
                    ByteArrayOutputStream().use { baos ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        png.writeBytes(baos.toByteArray())
                    }
                } else {
                    none.createNewFile()
                }
            }
            bmp
        }

    private fun renderIcon(info: LauncherActivityInfo, size: Int, mono: Boolean): Bitmap? {
        val d: Drawable = info.getIcon(activity.resources.displayMetrics.densityDpi)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val pad = size / 4
        if (mono) {
            if (Build.VERSION.SDK_INT < 33 || d !is AdaptiveIconDrawable) {
                bmp.recycle()
                return null
            }
            val m = d.monochrome?.mutate() ?: run {
                bmp.recycle()
                return null
            }
            m.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            m.setBounds(-pad, -pad, size + pad, size + pad)
            m.draw(canvas)
        } else if (d is AdaptiveIconDrawable) {
            val circle = Path().apply { addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW) }
            canvas.clipPath(circle)
            d.background?.let {
                it.setBounds(-pad, -pad, size + pad, size + pad)
                it.draw(canvas)
            }
            d.foreground?.let {
                it.setBounds(-pad, -pad, size + pad, size + pad)
                it.draw(canvas)
            }
        } else {
            d.setBounds(0, 0, size, size)
            d.draw(canvas)
        }
        return bmp
    }

    fun launch(key: String, bounds: Rect? = null): Boolean {
        val info = findActivity(key) ?: return false
        return runCatching {
            launcherApps?.startMainActivity(info.componentName, info.user, bounds, slideIn())
            true
        }.getOrDefault(false)
    }

    fun appInfo(key: String) {
        findActivity(key)?.let {
            runCatching { launcherApps?.startAppDetailsActivity(it.componentName, it.user, null, null) }
        }
    }

    fun uninstall(pkg: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { activity.startActivity(intent) }
    }

    fun isSystemApp(pkg: String): Boolean = try {
        val flags = activity.packageManager.getApplicationInfo(pkg, 0).flags
        flags and ApplicationInfo.FLAG_SYSTEM != 0
    } catch (_: Exception) {
        false
    }

    fun shortcuts(key: String): List<AppShortcut> {
        if (launcherApps?.hasShortcutHostPermission() != true) return emptyList()
        val info = findActivity(key) ?: return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(info.componentName.packageName)
            .setActivity(info.componentName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST,
            )
        val list: List<ShortcutInfo> = try {
            launcherApps.getShortcuts(query, info.user) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return list.filter { it.isEnabled }
            .sortedBy { it.rank }
            .take(4)
            .map { AppShortcut(it.id, (it.shortLabel ?: it.longLabel ?: "").toString()) }
    }

    fun startShortcut(key: String, id: String) {
        val info = findActivity(key) ?: return
        runCatching {
            launcherApps?.startShortcut(
                info.componentName.packageName,
                id,
                null,
                slideIn(),
                info.user,
            )
        }
    }

    // endregion

    // region System & Permissions

    private fun slideIn() = ActivityOptions.makeCustomAnimation(
        activity,
        R.anim.carino_app_enter,
        R.anim.carino_stay,
    ).toBundle()

    fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = activity.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roles.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = activity.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == activity.packageName
    }

    fun requestDefaultHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = activity.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                onRequestHomeRole?.invoke(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    @Suppress("WrongConstant")
    fun expandNotifications() {
        runCatching {
            val service = activity.getSystemService("statusbar")
            val manager = Class.forName("android.app.StatusBarManager")
            manager.getMethod("expandNotificationsPanel").invoke(service)
        }
    }

    fun hasNotificationAccess(): Boolean = CarinoNotifications.hasAccess(activity)

    fun requestNotificationAccess() {
        val component = ComponentName(activity, CarinoNotifications::class.java)
        val intent = if (Build.VERSION.SDK_INT >= 30) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    component.flattenToString(),
                )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
        runCatching { activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun hasLockAccess(): Boolean = CarinoLock.isOn(activity)

    fun lockScreen(): Boolean = CarinoLock.lock()

    fun requestLockAccess() {
        runCatching {
            activity.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun permissionFor(name: String): String = when (name) {
        "calendar" -> android.Manifest.permission.READ_CALENDAR
        "contacts" -> android.Manifest.permission.READ_CONTACTS
        "location" -> android.Manifest.permission.ACCESS_COARSE_LOCATION
        else -> throw IllegalArgumentException(name)
    }

    fun hasPermission(name: String): Boolean =
        activity.checkSelfPermission(permissionFor(name)) == PackageManager.PERMISSION_GRANTED

    fun requestPermission(name: String, onResult: (Boolean) -> Unit) {
        val perm = permissionFor(name)
        if (hasPermission(name)) {
            onResult(true)
            return
        }
        val handler = onRequestPermission
        if (handler != null) {
            handler.invoke(perm, onResult)
        } else {
            onResult(false)
        }
    }

    // endregion

    // region Media, Torch, Panels, Settings, Search

    fun mediaAction(action: String, positionMs: Long? = null) = mediaTracker.action(action, positionMs)

    fun mediaOpen() = mediaTracker.open()

    fun refreshMedia() = mediaTracker.refresh()

    fun toggleTorch(on: Boolean? = null): Boolean {
        val target = on ?: !_torchOn.value
        val id = runCatching {
            camera?.cameraIdList?.firstOrNull {
                camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull() ?: return false
        return runCatching {
            camera?.setTorchMode(id, target)
            true
        }.getOrDefault(false)
    }

    fun openPanel(name: String) {
        val action = if (Build.VERSION.SDK_INT >= 29) {
            when (name) {
                "wifi" -> Settings.Panel.ACTION_WIFI
                "internet" -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                "volume" -> Settings.Panel.ACTION_VOLUME
                "nfc" -> Settings.Panel.ACTION_NFC
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "dnd" -> Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
        } else {
            when (name) {
                "wifi", "internet" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "volume" -> Settings.ACTION_SOUND_SETTINGS
                "nfc" -> Settings.ACTION_NFC_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
        }
        runCatching { activity.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openNamedIntent(name: String) {
        val intent = when (name) {
            "clock" -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            "timer" -> Intent(AlarmClock.ACTION_SHOW_TIMERS)
            "alarm" -> Intent(AlarmClock.ACTION_SET_ALARM)
            "calendar" -> Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}"))
            "camera" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            "settings" -> Intent(Settings.ACTION_SETTINGS)
            "homeSettings" -> Intent(Settings.ACTION_HOME_SETTINGS)
            else -> return
        }
        runCatching { activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openSettingAction(action: String): Boolean = runCatching {
        activity.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    fun webSearch(query: String, url: String? = null) {
        val intent = if (url != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } else {
            Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query)
        }
        runCatching { activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openUrl(url: String) = webSearch("", url = url)

    // endregion

    // region Calendar, Contacts, Location

    suspend fun nextEvent(): CalendarEvent? = withContext(Dispatchers.IO) {
        if (!hasPermission("calendar")) return@withContext null
        val now = System.currentTimeMillis()
        val until = now + 36L * 60 * 60 * 1000
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, until)
        }.build()
        runCatching {
            activity.contentResolver.query(
                uri,
                arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                ),
                "${CalendarContract.Instances.END} > ? AND ${CalendarContract.Instances.ALL_DAY} = 0 " +
                    "AND ${CalendarContract.Instances.VISIBLE} = 1",
                arrayOf(now.toString()),
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                if (c.moveToFirst()) {
                    CalendarEvent(
                        title = c.getString(0) ?: "",
                        begin = c.getLong(1),
                        end = c.getLong(2),
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    suspend fun upNext(): UpNext = withContext(Dispatchers.IO) {
        val alarm = runCatching {
            activity.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
        }.getOrNull()
        val events = if (!hasPermission("calendar")) {
            emptyList()
        } else {
            val now = System.currentTimeMillis()
            val endOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
                ContentUris.appendId(it, now)
                ContentUris.appendId(it, endOfDay)
            }.build()
            val out = mutableListOf<UpNextEvent>()
            runCatching {
                activity.contentResolver.query(
                    uri,
                    arrayOf(
                        CalendarContract.Instances.TITLE,
                        CalendarContract.Instances.BEGIN,
                        CalendarContract.Instances.END,
                        CalendarContract.Instances.ALL_DAY,
                        CalendarContract.Instances.EVENT_LOCATION,
                    ),
                    "${CalendarContract.Instances.END} > ? AND ${CalendarContract.Instances.VISIBLE} = 1",
                    arrayOf(now.toString()),
                    "${CalendarContract.Instances.ALL_DAY} DESC, ${CalendarContract.Instances.BEGIN} ASC",
                )?.use { c ->
                    while (c.moveToNext() && out.size < 4) {
                        out.add(
                            UpNextEvent(
                                title = c.getString(0) ?: "",
                                begin = c.getLong(1),
                                end = c.getLong(2),
                                allDay = c.getInt(3) == 1,
                                location = c.getString(4) ?: "",
                            ),
                        )
                    }
                }
            }
            out
        }
        UpNext(events = events, alarm = alarm)
    }

    suspend fun searchContacts(query: String): List<ContactHit> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !hasPermission("contacts")) return@withContext emptyList()
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(query))
        val out = mutableListOf<ContactHit>()
        runCatching {
            activity.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ),
                null,
                null,
                null,
            )?.use { c ->
                while (c.moveToNext() && out.size < 4) {
                    out.add(ContactHit(c.getLong(0), c.getString(1), c.getString(2) ?: ""))
                }
            }
        }
        out
    }

    fun openContact(c: ContactHit) {
        val uri = ContactsContract.Contacts.getLookupUri(c.id, c.lookup)
        runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun lastLocation(): WeatherPlace? {
        if (!hasPermission("location")) return null
        val lm = activity.getSystemService(LocationManager::class.java) ?: return null
        val best = lm.getProviders(true)
            .mapNotNull {
                try {
                    lm.getLastKnownLocation(it)
                } catch (_: SecurityException) {
                    null
                }
            }
            .maxByOrNull { it.time } ?: return null
        return WeatherPlace(name = "", lat = best.latitude, lon = best.longitude)
    }

    // endregion

    // region Wallpaper & Photos

    private fun fitWallpaperToScreen() {
        runCatching {
            val (w, h) = screenPixels()
            WallpaperManager.getInstance(activity).suggestDesiredDimensions(w, h)
        }
    }

    fun screenPixels(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= 30) {
            val bounds = activity.windowManager.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val m = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
    }

    fun pickImage(onPicked: (String?) -> Unit) {
        val handler = onPickImageRequest
        if (handler != null) {
            handler.invoke(onPicked)
        } else {
            onPicked(null)
        }
    }

    suspend fun copyToWallpapers(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(activity.filesDir, "wallpapers").apply { mkdirs() }
            val file = File(dir, "photo-${System.currentTimeMillis()}.img")
            activity.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            file.absolutePath
        }.getOrNull()
    }

    fun bakedPath(photo: String, blur: Double): String =
        "$photo.b${(blur * 100).roundToInt()}.png"

    fun blurReady(photo: String, blur: Double): Boolean =
        File(bakedPath(photo, blur)).exists()

    suspend fun bakeBlur(photo: String, blur: Double): File? = withContext(Dispatchers.IO) {
        if (blur <= 0.01) return@withContext null
        val out = File(bakedPath(photo, blur))
        if (out.exists()) return@withContext out
        val src = decodeSampledBitmap(photo, 720, 1280) ?: return@withContext null
        val radius = (blur * 36.0).roundToInt().coerceIn(1, 50)
        val blurred = fastBlur(src, radius)
        if (blurred !== src) src.recycle()
        out.outputStream().use { fos ->
            blurred.compress(Bitmap.CompressFormat.PNG, 95, fos)
        }
        blurred.recycle()
        out
    }

    suspend fun deletePhoto(photo: String) = withContext(Dispatchers.IO) {
        val file = File(photo)
        val dir = file.parentFile ?: return@withContext
        val prefix = file.name
        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.name.startsWith(prefix)) {
                runCatching { f.delete() }
            }
        }
    }

    suspend fun applySystemWallpaper(layout: HomeLayout, which: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val (w, h) = screenPixels()
                val bmp = renderWallpaperBitmap(layout, w.coerceAtLeast(360), h.coerceAtLeast(640), soft = which == "home")
                    ?: return@withContext false
                val flags = when (which) {
                    "home" -> WallpaperManager.FLAG_SYSTEM
                    "lock" -> WallpaperManager.FLAG_LOCK
                    else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                val bytes = ByteArrayOutputStream().use { baos ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 95, baos)
                    bmp.recycle()
                    baos.toByteArray()
                }
                bytes.inputStream().use { input ->
                    WallpaperManager.getInstance(activity).setStream(input, null, true, flags)
                }
                true
            }.getOrDefault(false)
        }

    private fun renderWallpaperBitmap(
        layout: HomeLayout,
        width: Int,
        height: Int,
        soft: Boolean,
    ): Bitmap? {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val photo = layout.wallpaper.photo
        if (photo != null) {
            val path = if (layout.blur > 0.01 && blurReady(photo, layout.blur)) {
                bakedPath(photo, layout.blur)
            } else {
                photo
            }
            val src = decodeSampledBitmap(path, if (soft) width / 4 else width, if (soft) height / 4 else height)
                ?: return null
            val scale = max(width.toFloat() / src.width, height.toFloat() / src.height)
            val dw = src.width * scale
            val dh = src.height * scale
            val left = (width - dw) / 2f
            val top = (height - dh) / 2f
            canvas.drawBitmap(src, null, RectF(left, top, left + dw, top + dh), Paint(Paint.FILTER_BITMAP_FLAG))
            src.recycle()
        } else {
            val palette = VWallpapers.all[layout.wallpaper.builtin] ?: VWallpapers.night
            canvas.drawColor(palette.baseInt)
            val longer = max(width, height).toFloat()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            for (b in palette.blobs) {
                val cx = width / 2f + b.x * width / 2f
                val cy = height / 2f + b.y * height / 2f
                val r = (b.radius * longer).coerceAtLeast(1f)
                paint.shader = RadialGradient(
                    cx,
                    cy,
                    r,
                    intArrayOf(b.colorInt, b.colorInt and 0x00FFFFFF),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(cx, cy, r, paint)
            }
        }
        if (layout.dim > 0.0) {
            val alpha = (layout.dim.coerceIn(0.0, 0.8) * 255).roundToInt()
            canvas.drawColor(Color.argb(alpha, 0, 0, 0))
        }
        return if (soft) {
            val smallW = (width / 6).coerceAtLeast(32)
            val smallH = (height / 6).coerceAtLeast(32)
            val small = Bitmap.createScaledBitmap(bmp, smallW, smallH, true)
            val blurred = fastBlur(small, 16)
            if (blurred !== small) small.recycle()
            val finalBmp = Bitmap.createScaledBitmap(blurred, width, height, true)
            if (finalBmp !== blurred) blurred.recycle()
            bmp.recycle()
            finalBmp
        } else {
            bmp
        }
    }

    fun decodeSampledBitmap(path: String, reqW: Int, reqH: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        var inSampleSize = 1
        while (opts.outHeight / inSampleSize > reqH * 2 && opts.outWidth / inSampleSize > reqW * 2) {
            inSampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, decodeOpts)
    }

    /** Fast integer stack blur for baked blurred wallpaper photos. */
    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return sentBitmap
        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val vmin = IntArray(max(w, h))
        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (i in dv.indices) dv[i] = i / divsum
        var yw = 0
        var yi = 0
        val stack = Array(div) { IntArray(3) }
        val r1 = radius + 1

        for (y in 0 until h) {
            var rinsum = 0
            var ginsum = 0
            var binsum = 0
            var routsum = 0
            var goutsum = 0
            var boutsum = 0
            var rsum = 0
            var gsum = 0
            var bsum = 0
            for (i in -radius..radius) {
                val p = pix[yi + min(wm, max(i, 0))]
                val sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                val rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            var stackpointer = radius
            for (x in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                val stackstart = stackpointer - radius + div
                var sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) vmin[x] = min(x + radius + 1, wm)
                val p = pix[yw + vmin[x]]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            var rinsum = 0
            var ginsum = 0
            var binsum = 0
            var routsum = 0
            var goutsum = 0
            var boutsum = 0
            var rsum = 0
            var gsum = 0
            var bsum = 0
            var yp = -radius * w
            for (i in -radius..radius) {
                yi = max(0, yp) + x
                val sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                val rbs = r1 - kotlin.math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) yp += w
            }
            yi = x
            var stackpointer = radius
            for (y in 0 until h) {
                pix[yi] = (0xff shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                val stackstart = stackpointer - radius + div
                var sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) vmin[y] = min(y + r1, hm) * w
                val p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    // endregion
}
