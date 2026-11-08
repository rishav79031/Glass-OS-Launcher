package com.glasslauncher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class WeatherPlaceSuggestion(
    val name: String,
    val subtitle: String,
    val placeKey: String,
)

/**
 * Current weather from MET Norway (api.met.no): free, no key, and allowed in commercial apps with attribution
 * (CC BY 4.0). Refreshes at most every 30 minutes and caches the last result, so the widget shows instantly and
 * the launcher stays light. Coordinates are rounded to ~1 km.
 */
class WeatherRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(readCache() ?: defaultNewDelhiWeather())
    val weather: StateFlow<Weather?> = state.asStateFlow()

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** [manualPlace] is "lat|lon|name"; without it the device's coarse location or New Delhi fallback is used. */
    suspend fun refresh(manualPlace: String?, force: Boolean = false) = withContext(Dispatchers.IO) {
        val cached = state.value
        if (!force && cached != null && System.currentTimeMillis() - cached.fetchedAt < REFRESH_MS) return@withContext
        val (lat, lon, place) = parsePlace(manualPlace) ?: deviceLocation() ?: DEFAULT_PLACE
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=${round(lat)}&lon=${round(lon)}"
        val series = runCatching { JSONObject(get(url)).getJSONObject("properties").getJSONArray("timeseries") }.getOrNull()
        if (series == null) return@withContext
        val now = series.optJSONObject(0) ?: return@withContext
        val data = now.getJSONObject("data")
        val symbol = (data.optJSONObject("next_1_hours") ?: data.optJSONObject("next_6_hours"))
            ?.optJSONObject("summary")?.optString("symbol_code").orEmpty()
        // Today's high and low from the next 24 hourly readings.
        val temps = (0 until minOf(24, series.length())).mapNotNull {
            series.optJSONObject(it)?.optJSONObject("data")?.optJSONObject("instant")?.optJSONObject("details")?.optDouble("air_temperature")
        }.filter { !it.isNaN() }
        val result = Weather(
            place = place,
            temperature = data.getJSONObject("instant").getJSONObject("details").optDouble("air_temperature").roundToInt(),
            high = (temps.maxOrNull() ?: 0.0).roundToInt(),
            low = (temps.minOrNull() ?: 0.0).roundToInt(),
            code = symbolToCode(symbol),
            isDay = when {
                symbol.endsWith("_night") -> false
                symbol.endsWith("_day") -> true
                else -> java.time.LocalTime.now().hour in 6..18
            },
            fetchedAt = System.currentTimeMillis(),
            hourly = (1..6).mapNotNull { index -> series.optJSONObject(index)?.let(::hourOf) },
        )
        state.value = result
        writeCache(result)
    }

    private fun defaultNewDelhiWeather(): Weather = Weather(
        place = "New Delhi",
        temperature = 28,
        high = 32,
        low = 22,
        code = 0,
        isDay = java.time.LocalTime.now().hour in 6..18,
        fetchedAt = 0L,
        hourly = (1..6).map {
            val h = (java.time.LocalTime.now().hour + it) % 24
            HourForecast(h, 28 + (it % 3), 0, h in 6..18)
        }
    )

    private fun hourOf(entry: JSONObject): HourForecast? = runCatching {
        val time = java.time.Instant.parse(entry.getString("time")).atZone(java.time.ZoneId.systemDefault())
        val data = entry.getJSONObject("data")
        val symbol = data.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code").orEmpty()
        HourForecast(
            hour = time.hour,
            temperature = data.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature").roundToInt(),
            code = symbolToCode(symbol),
            isDay = if (symbol.endsWith("_night")) false else symbol.endsWith("_day") || time.hour in 6..18,
        )
    }.getOrNull()

    /** Looks up city suggestions matching [query]. Returns up to 6 results. */
    @Suppress("DEPRECATION")
    suspend fun searchSuggestions(query: String): List<WeatherPlaceSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val results = mutableListOf<WeatherPlaceSuggestion>()

        // 1. Android system Geocoder
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val list = geocoder.getFromLocationName(trimmed, 5).orEmpty()
            for (addr in list) {
                val city = addr.locality ?: addr.subAdminArea ?: addr.featureName ?: continue
                val subParts = listOfNotNull(addr.adminArea, addr.countryName).filter { it != city }
                val subtitle = subParts.joinToString(", ")
                val key = "${addr.latitude}|${addr.longitude}|$city"
                results.add(WeatherPlaceSuggestion(name = city, subtitle = subtitle, placeKey = key))
            }
        }

        // 2. Open-Meteo geocoding API (free, fast, reliable fallback/supplement)
        if (results.size < 5) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
                val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json"
                val json = JSONObject(get(url))
                val array = json.optJSONArray("results")
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.getString("name")
                        val admin1 = obj.optString("admin1")
                        val country = obj.optString("country")
                        val lat = obj.getDouble("latitude")
                        val lon = obj.getDouble("longitude")
                        val subtitle = listOfNotNull(admin1.takeIf { it.isNotBlank() }, country.takeIf { it.isNotBlank() }).joinToString(", ")
                        val key = "$lat|$lon|$name"
                        results.add(WeatherPlaceSuggestion(name = name, subtitle = subtitle, placeKey = key))
                    }
                }
            }
        }

        results.distinctBy { it.placeKey }
    }

    /** Looks up a city name; returns "lat|lon|name" or null. */
    @Suppress("DEPRECATION")
    suspend fun search(name: String): String? = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext null
        val direct = searchSuggestions(trimmed).firstOrNull()?.placeKey
        if (direct != null) return@withContext direct
        val address = runCatching { Geocoder(context, Locale.getDefault()).getFromLocationName(trimmed, 1)?.firstOrNull() }.getOrNull()
            ?: return@withContext null
        "${address.latitude}|${address.longitude}|${address.locality ?: address.featureName ?: trimmed}"
    }

    private fun parsePlace(place: String?): Triple<Double, Double, String>? {
        val parts = place?.split("|") ?: return null
        if (parts.size < 3) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null
        return Triple(lat, lon, parts[2])
    }

    @SuppressLint("MissingPermission")
    private suspend fun deviceLocation(): Triple<Double, Double, String>? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val last = manager.getProviders(true)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        // No cached fix yet: ask the best enabled provider for one (fused, then network, then GPS).
        val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        val location = last ?: provider?.let {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    runCatching {
                        manager.getCurrentLocation(it, null, context.mainExecutor) { fix -> continuation.resume(fix) }
                    }.onFailure { continuation.resume(null) }
                }
            }
        } ?: return null
        return Triple(location.latitude, location.longitude, placeName(location))
    }

    @Suppress("DEPRECATION")
    private fun placeName(location: Location): String = runCatching {
        val address = Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
        address?.locality ?: address?.subAdminArea ?: address?.adminArea
    }.getOrNull() ?: "My Location"

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        // MET Norway asks every client to identify itself.
        connection.setRequestProperty("User-Agent", "GlassLauncher/1.1 com.glasslauncher")
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun readCache(): Weather? {
        if (!prefs.contains(KEY_FETCHED)) return null
        return Weather(
            place = prefs.getString(KEY_PLACE, null) ?: return null,
            temperature = prefs.getInt(KEY_TEMP, 0),
            high = prefs.getInt(KEY_HIGH, 0),
            low = prefs.getInt(KEY_LOW, 0),
            code = prefs.getInt(KEY_CODE, 0),
            isDay = prefs.getBoolean(KEY_DAY, true),
            fetchedAt = prefs.getLong(KEY_FETCHED, 0L),
            hourly = prefs.getString(KEY_HOURLY, null).orEmpty().split(";").mapNotNull { entry ->
                val n = entry.split(",").mapNotNull { it.toIntOrNull() }
                if (n.size < 4) null else HourForecast(n[0], n[1], n[2], n[3] == 1)
            },
        )
    }

    private fun writeCache(weather: Weather) = prefs.edit {
        putString(KEY_PLACE, weather.place)
        putInt(KEY_TEMP, weather.temperature)
        putInt(KEY_HIGH, weather.high)
        putInt(KEY_LOW, weather.low)
        putInt(KEY_CODE, weather.code)
        putBoolean(KEY_DAY, weather.isDay)
        putLong(KEY_FETCHED, weather.fetchedAt)
        putString(KEY_HOURLY, weather.hourly.joinToString(";") { "${it.hour},${it.temperature},${it.code},${if (it.isDay) 1 else 0}" })
    }

    private fun round(value: Double): String = String.format(Locale.US, "%.2f", value)

    companion object {
        val DEFAULT_PLACE = Triple(28.6139, 77.2090, "New Delhi")
        const val REFRESH_MS = 30 * 60 * 1000L
        const val KEY_PLACE = "place"
        const val KEY_TEMP = "temp"
        const val KEY_HIGH = "high"
        const val KEY_LOW = "low"
        const val KEY_CODE = "code"
        const val KEY_DAY = "day"
        const val KEY_FETCHED = "fetched"
        const val KEY_HOURLY = "hourly"
    }
}

/** WMO weather code → iOS-style description. */
fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear"
    1 -> "Mostly Clear"
    2 -> "Partly Cloudy"
    3 -> "Cloudy"
    45, 48 -> "Foggy"
    in 51..57 -> "Drizzle"
    in 61..67 -> "Rain"
    in 71..77 -> "Snow"
    in 80..82 -> "Showers"
    85, 86 -> "Snow Showers"
    in 95..99 -> "Thunderstorms"
    else -> "—"
}

/** MET Norway symbol codes → the WMO-style codes the widget uses for its icon and description. */
private fun symbolToCode(symbol: String): Int {
    val base = symbol.substringBefore("_")
    return when {
        base == "clearsky" -> 0
        base == "fair" -> 1
        base == "partlycloudy" -> 2
        base == "cloudy" -> 3
        base == "fog" -> 45
        "thunder" in base -> 95
        "snow" in base || "sleet" in base -> 71
        "showers" in base -> 80
        "drizzle" in base -> 51
        "rain" in base -> 61
        else -> 3
    }
}
