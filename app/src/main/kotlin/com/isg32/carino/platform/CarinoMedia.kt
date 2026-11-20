package com.isg32.carino.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.isg32.carino.data.MediaState

/**
 * Active media session tracking for the Now playing widget.
 * Only listens while Cariño is on screen.
 */
class CarinoMedia(
    private val context: Context,
    private val onUpdate: (MediaState?) -> Unit,
) {
    private val sessions = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, CarinoNotifications::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var listening = false
    private var watched: List<MediaController> = emptyList()
    private val seenPlaying = mutableSetOf<MediaSession.Token>()

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        choose(list ?: emptyList())
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = choose(watched)
        override fun onMetadataChanged(metadata: MediaMetadata?) = choose(watched)
        override fun onSessionDestroyed() = refresh()
    }

    fun start() {
        if (listening || !CarinoNotifications.hasAccess(context)) {
            if (!listening) publish()
            return
        }
        try {
            sessions?.addOnActiveSessionsChangedListener(sessionsChanged, component, main)
            listening = true
            refresh()
        } catch (_: SecurityException) {
            publish()
        }
    }

    fun stop() {
        if (!listening) return
        listening = false
        try {
            sessions?.removeOnActiveSessionsChangedListener(sessionsChanged)
        } catch (_: Exception) {
        }
        for (c in watched) c.unregisterCallback(callback)
        watched = emptyList()
        controller = null
    }

    fun refresh() {
        if (!CarinoNotifications.hasAccess(context)) {
            controller = null
            publish()
            return
        }
        val list = try {
            sessions?.getActiveSessions(component) ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
        choose(list)
    }

    private fun choose(list: List<MediaController>) {
        if (list.map { it.sessionToken } != watched.map { it.sessionToken }) {
            for (c in watched) c.unregisterCallback(callback)
            for (c in list) c.registerCallback(callback, main)
            watched = list
        }
        val real = list.filter { !it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() }
        val playing = real.filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        playing.forEach { seenPlaying.add(it.sessionToken) }
        seenPlaying.retainAll(list.map { it.sessionToken }.toSet())
        controller = playing.firstOrNull()
            ?: real.firstOrNull { it.sessionToken in seenPlaying }
        publish()
    }

    fun snapshot(): MediaState? {
        val c = controller ?: return null
        val meta = c.metadata
        val state = c.playbackState
        val app = try {
            context.getSystemService(LauncherApps::class.java)
                ?.getActivityList(c.packageName, Process.myUserHandle())
                ?.firstOrNull()?.label?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }
        var position = state?.position ?: 0L
        if (state != null && state.state == PlaybackState.STATE_PLAYING && state.lastPositionUpdateTime > 0) {
            position += ((SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed).toLong()
        }
        return MediaState(
            pkg = c.packageName,
            app = app,
            title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: "",
            durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = position.coerceAtLeast(0L),
            playing = state?.state == PlaybackState.STATE_PLAYING,
            canSeek = ((state?.actions ?: 0L) and PlaybackState.ACTION_SEEK_TO) != 0L,
            atMs = System.currentTimeMillis(),
        )
    }

    private fun publish() {
        onUpdate(snapshot())
    }

    fun action(name: String, position: Long? = null) {
        val t = controller?.transportControls ?: return
        when (name) {
            "play" -> t.play()
            "pause" -> t.pause()
            "next" -> t.skipToNext()
            "previous" -> t.skipToPrevious()
            "seek" -> if (position != null) t.seekTo(position)
        }
    }

    fun open() {
        val c = controller ?: return
        c.sessionActivity?.let {
            try {
                it.send()
                return
            } catch (_: Exception) {
            }
        }
        context.packageManager.getLaunchIntentForPackage(c.packageName)?.let {
            context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
