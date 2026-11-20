package com.isg32.carino.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom

class CarinoPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("carino_prefs", Context.MODE_PRIVATE)

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun readSettings(): CarinoSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return CarinoSettings()
        return try {
            offerNewWidgets(json.decodeFromString<CarinoSettings>(raw))
        } catch (_: Exception) {
            CarinoSettings()
        }
    }

    fun writeSettings(settings: CarinoSettings) {
        prefs.edit().putString(KEY_SETTINGS, json.encodeToString(settings)).apply()
    }

    private fun offerNewWidgets(s: CarinoSettings): CarinoSettings {
        val fresh = BuiltinWidget.entries
            .map { it.name }
            .filter { it !in s.knownWidgets }
        if (fresh.isEmpty()) return s
        val at = if (s.widgets.isEmpty()) 0 else 1
        val nextWidgets = s.widgets.take(at) + fresh + s.widgets.drop(at)
        return s.copy(
            widgets = nextWidgets,
            knownWidgets = s.knownWidgets + fresh,
        )
    }

    fun readLayouts(): LayoutsState {
        val raw = prefs.getString(KEY_LAYOUTS, null) ?: return LayoutsState()
        return try {
            val loaded = json.decodeFromString<LayoutsState>(raw)
            if (loaded.layouts.isEmpty()) LayoutsState() else loaded
        } catch (_: Exception) {
            LayoutsState()
        }
    }

    fun writeLayouts(layouts: LayoutsState) {
        prefs.edit().putString(KEY_LAYOUTS, json.encodeToString(layouts)).apply()
    }

    fun readAppsCache(): List<AppEntry>? {
        val raw = prefs.getString(KEY_APPS_CACHE, null) ?: return null
        return try {
            json.decodeFromString<List<AppEntry>>(raw).takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    fun writeAppsCache(apps: List<AppEntry>) {
        prefs.edit().putString(KEY_APPS_CACHE, json.encodeToString(apps)).apply()
    }

    fun readWeatherCache(place: WeatherPlace?, fahrenheit: Boolean): Weather? {
        val raw = prefs.getString(KEY_WEATHER_CACHE, null) ?: return null
        val key = weatherCacheKey(place, fahrenheit)
        return try {
            val wrapper = json.decodeFromString<WeatherCacheEntry>(raw)
            if (wrapper.key == key) wrapper.weather else null
        } catch (_: Exception) {
            null
        }
    }

    fun writeWeatherCache(place: WeatherPlace?, fahrenheit: Boolean, weather: Weather) {
        val entry = WeatherCacheEntry(weatherCacheKey(place, fahrenheit), weather)
        prefs.edit().putString(KEY_WEATHER_CACHE, json.encodeToString(entry)).apply()
    }

    private fun weatherCacheKey(place: WeatherPlace?, fahrenheit: Boolean): String =
        "${place?.name}|${place?.lat}|${place?.lon}|$fahrenheit"

    fun exportAll(settings: CarinoSettings, layouts: LayoutsState): String =
        json.encodeToString(CarinoExport(version = 1, settings = settings, layouts = layouts))

    fun parseImport(raw: String): CarinoExport? = try {
        json.decodeFromString<CarinoExport>(raw)
    } catch (_: Exception) {
        null
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    @kotlinx.serialization.Serializable
    private data class WeatherCacheEntry(
        val key: String,
        val weather: Weather,
    )

    companion object {
        private const val KEY_SETTINGS = "settings_v1"
        private const val KEY_LAYOUTS = "layouts_v1"
        private const val KEY_APPS_CACHE = "apps_cache_v1"
        private const val KEY_WEATHER_CACHE = "weather_cache_v1"

        fun newSalt(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun hashPasscode(pin: String, salt: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
