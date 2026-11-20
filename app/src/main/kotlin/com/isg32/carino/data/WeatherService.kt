package com.isg32.carino.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Open-Meteo weather fetching and city geocoding. Only invoked when the user opts into weather.
 */
object WeatherService {

    suspend fun fetch(place: WeatherPlace, fahrenheit: Boolean): Weather = withContext(Dispatchers.IO) {
        val unit = if (fahrenheit) "fahrenheit" else "celsius"
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.lat}&longitude=${place.lon}" +
            "&current=temperature_2m,weather_code&temperature_unit=$unit"
        val body = getText(url)
        val json = JSONObject(body)
        val current = json.getJSONObject("current")
        Weather(
            temp = current.getDouble("temperature_2m"),
            code = current.getInt("weather_code"),
            fahrenheit = fahrenheit,
            at = System.currentTimeMillis(),
            place = place.name,
        )
    }

    suspend fun searchPlaces(query: String): List<WeatherPlace> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        val encoded = URLEncoder.encode(q, "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=en&format=json"
        val body = getText(url)
        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                val admin = item.optString("admin1")
                val country = item.optString("country")
                val subtitle = listOf(admin, country).filter { it.isNotBlank() }.joinToString(", ")
                add(
                    WeatherPlace(
                        name = name,
                        lat = item.getDouble("latitude"),
                        lon = item.getDouble("longitude"),
                        subtitle = subtitle,
                    ),
                )
            }
        }
    }

    private fun getText(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
        71, 73, 75, 77, 85, 86 -> "Snow"
        95, 96, 99 -> "Storm"
        else -> "Weather"
    }
}
