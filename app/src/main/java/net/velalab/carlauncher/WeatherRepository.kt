package net.velalab.carlauncher

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(val temperature: Double, val code: Int, val tomorrowHigh: Double?, val tomorrowLow: Double?, val updatedAt: Long, val cached: Boolean = false)

class WeatherRepository(context: Context) {
    private val prefs = context.getSharedPreferences("vela_weather", Context.MODE_PRIVATE)
    fun cached(): WeatherData? = runCatching {
        WeatherData(prefs.getString("temperature", null)!!.toDouble(), prefs.getInt("code", 0),
            prefs.getString("high", null)?.toDouble(), prefs.getString("low", null)?.toDouble(),
            prefs.getLong("updated", 0), true)
    }.getOrNull()

    fun fetch(latitude: Double, longitude: Double): WeatherData {
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&forecast_days=2&timezone=auto")
        val connection = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
        try {
            if (connection.responseCode != 200) error("Weather HTTP ${connection.responseCode}")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            val highs = daily.getJSONArray("temperature_2m_max")
            val lows = daily.getJSONArray("temperature_2m_min")
            val value = WeatherData(current.getDouble("temperature_2m"), current.getInt("weather_code"),
                if (highs.length() > 1) highs.getDouble(1) else null,
                if (lows.length() > 1) lows.getDouble(1) else null, System.currentTimeMillis())
            prefs.edit().putString("temperature", value.temperature.toString()).putInt("code", value.code)
                .putString("high", value.tomorrowHigh?.toString()).putString("low", value.tomorrowLow?.toString())
                .putLong("updated", value.updatedAt).apply()
            return value
        } finally { connection.disconnect() }
    }
}

fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear sky"; 1, 2 -> "Partly cloudy"; 3 -> "Cloudy"; 45, 48 -> "Fog"
    in 51..67 -> "Rain"; in 71..77 -> "Snow"; in 80..82 -> "Showers"
    in 95..99 -> "Thunderstorm"; else -> "Weather available"
}
