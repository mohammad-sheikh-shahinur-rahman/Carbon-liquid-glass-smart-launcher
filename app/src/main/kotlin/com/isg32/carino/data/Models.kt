package com.isg32.carino.data

import kotlinx.serialization.Serializable

@Serializable
data class AppEntry(
    val key: String,
    val pkg: String,
    val label: String,
    val installedAt: Long,
    val work: Boolean = false,
    val paused: Boolean = false,
) {
    val section: String
        get() {
            if (label.isEmpty()) return "#"
            val c = label[0].uppercaseChar()
            return if (c in 'A'..'Z') c.toString() else "#"
        }

    fun isNew(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs - installedAt < 2L * 24 * 60 * 60 * 1000
}

data class AppShortcut(
    val id: String,
    val label: String,
)

@Serializable
enum class InkMode { white, black, auto }

@Serializable
enum class IconStyle { mono, colored }

@Serializable
data class WallpaperRef(
    val builtin: String? = null,
    val photo: String? = null,
) {
    companion object {
        fun builtin(name: String) = WallpaperRef(builtin = name, photo = null)
        fun photo(path: String) = WallpaperRef(builtin = null, photo = path)
    }
}

@Serializable
data class HomeLayout(
    val id: String,
    val name: String,
    val wallpaper: WallpaperRef = WallpaperRef.builtin("carbon"),
    val dim: Double = 0.0,
    val blur: Double = 0.0,
    val ink: InkMode = InkMode.white,
    val glass: Double = 0.65,
    val icons: IconStyle = IconStyle.colored,
    val iconBlur: Boolean = true,
    val columns: Int = 4,
    val favRows: Int = 2,
    val favourites: List<String?> = emptyList(),
    val dock: List<String?> = emptyList(),
) {
    val favSlots: Int get() = columns * favRows

    val favGrid: List<String?>
        get() = List(favSlots) { i -> favourites.getOrNull(i) }

    val dockRow: List<String?>
        get() = List(columns) { i -> dock.getOrNull(i) }
}

@Serializable
enum class AppsView { list, grid }

@Serializable
enum class SearchMode { apps, essentials }

@Serializable
enum class GestureAction { search, notifications, customise, apps, lock, nothing }

@Serializable
enum class BuiltinWidget { media, upNext, clock, weather, battery, calendar, notes }

object WidgetKey {
    fun hosted(id: Int): String = "aw:$id"

    fun hostedId(key: String): Int? =
        if (key.startsWith("aw:")) key.substring(3).toIntOrNull() else null

    fun builtin(key: String): BuiltinWidget? =
        BuiltinWidget.entries.firstOrNull { it.name == key }
}

@Serializable
data class WeatherPlace(
    val name: String,
    val lat: Double,
    val lon: Double,
    val subtitle: String = "",
)

@Serializable
data class Weather(
    val temp: Double,
    val code: Int,
    val fahrenheit: Boolean,
    val at: Long,
    val place: String,
    val high: Double? = null,
    val low: Double? = null,
    val precipChance: Int? = null,
)

data class BatteryState(
    val level: Int,
    val charging: Boolean,
)

data class CalendarEvent(
    val title: String,
    val begin: Long,
    val end: Long,
) {
    fun ongoing(nowMs: Long = System.currentTimeMillis()): Boolean = nowMs > begin
}

data class UpNextEvent(
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String,
)

data class UpNext(
    val events: List<UpNextEvent> = emptyList(),
    val alarm: Long? = null,
)

data class ContactHit(
    val id: Long,
    val lookup: String,
    val name: String,
)

data class MediaState(
    val pkg: String,
    val app: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val positionMs: Long,
    val playing: Boolean,
    val canSeek: Boolean,
    val atMs: Long = System.currentTimeMillis(),
) {
    fun positionAt(nowMs: Long = System.currentTimeMillis()): Long {
        if (!playing) return positionMs
        val p = positionMs + (nowMs - atMs).coerceAtLeast(0L)
        return if (durationMs > 0L && p > durationMs) durationMs else p
    }
}

data class WidgetProviderInfo(
    val provider: String,
    val user: Long,
    val pkg: String,
    val label: String,
    val app: String,
    val minWidth: Int,
    val minHeight: Int,
    val configurable: Boolean,
)

@Serializable
data class CarinoSettings(
    val onboarded: Boolean = false,
    val appsView: AppsView = AppsView.list,
    val newBadges: Boolean = true,
    val scrubberHaptics: Boolean = true,
    val searchWeb: Boolean = true,
    val webEngine: String = "DuckDuckGo",
    val searchOpensOn: SearchMode = SearchMode.apps,
    val enterLaunches: Boolean = true,
    val haptics: Boolean = true,
    val clock24: Boolean = true,
    val showBattery: Boolean = true,
    val showDate: Boolean = true,
    val swipeUp: GestureAction = GestureAction.search,
    val swipeDown: GestureAction = GestureAction.notifications,
    val doubleTap: GestureAction = GestureAction.lock,
    val hidden: Set<String> = emptySet(),
    val hiddenFindByName: Boolean = false,
    val essentials: List<String> = listOf(
        "wifi",
        "bluetooth",
        "torch",
        "dnd",
        "timer",
        "alarm",
        "camera",
    ),
    val widgets: List<String> = listOf("media", "clock", "battery", "calendar", "notes"),
    val knownWidgets: List<String> = listOf("media", "clock", "battery", "calendar", "notes"),
    val widgetRows: Map<String, Int> = emptyMap(),
    val note: String = "",
    val photos: List<String> = emptyList(),
    val dots: Boolean = true,
    val showNextEvent: Boolean = false,
    val searchContacts: Boolean = false,
    val searchSettings: Boolean = true,
    val weatherOn: Boolean = false,
    val weatherPlace: WeatherPlace? = null,
    val weatherFahrenheit: Boolean = false,
    val vaultHash: String? = null,
    val vaultSalt: String? = null,
) {
    val hasPasscode: Boolean get() = vaultHash != null && vaultSalt != null
}

@Serializable
data class LayoutsState(
    val layouts: List<HomeLayout> = listOf(HomeLayout(id = "carbon", name = "Carbon")),
    val activeId: String = "carbon",
    val seeded: Boolean = false,
) {
    val active: HomeLayout
        get() = layouts.firstOrNull { it.id == activeId } ?: layouts.first()

    val activeIndex: Int
        get() = layouts.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
}

@Serializable
data class CarinoExport(
    val version: Int = 1,
    val settings: CarinoSettings,
    val layouts: LayoutsState,
)
