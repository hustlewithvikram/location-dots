package com.locationdots.app.feature.place

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class LocationSearchResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)

class LocationSearchRepository {
    suspend fun search(query: String): List<LocationSearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        val encoded = URLEncoder.encode(cleanQuery, Charsets.UTF_8.name())
        val url = URL(
            "https://nominatim.openstreetmap.org/search" +
                "?q=$encoded&format=jsonv2&limit=6&addressdetails=1"
        )

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LocationDots/0.1 (Android)")
        }

        try {
            if (connection.responseCode !in 200..299) return@withContext emptyList()

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val results = JSONArray(body)
            buildList {
                for (index in 0 until results.length()) {
                    val item = results.optJSONObject(index) ?: continue
                    val lat = item.optString("lat").toDoubleOrNull() ?: continue
                    val lon = item.optString("lon").toDoubleOrNull() ?: continue
                    val displayName = item.optString("display_name").trim()
                    if (displayName.isEmpty()) continue

                    val shortName = item.optString("name")
                        .trim()
                        .ifEmpty { displayName.substringBefore(",").trim() }
                        .ifEmpty { "Selected location" }

                    add(
                        LocationSearchResult(
                            name = shortName,
                            latitude = lat,
                            longitude = lon,
                            address = displayName
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
