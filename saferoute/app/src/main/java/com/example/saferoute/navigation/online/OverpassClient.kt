package com.example.saferoute.navigation.online

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object OverpassClient {
    private val client = OkHttpClient()

    data class SafetyPoint(val lat: Double, val lon: Double)

    // Query Overpass with bbox and return list of safety points
    fun querySafetyPoints(bbox: String, timeoutSeconds: Int = 25): List<SafetyPoint> {
        val query = """
[out:json][timeout:$timeoutSeconds];
(
  node["highway"="street_lamp"]($bbox);
  node["man_made"="street_lamp"]($bbox);
  node["tourism"]($bbox);
  node["historic"]($bbox);
  node["amenity"~"place_of_worship|library|museum|school|clinic|hospital"]($bbox);
);
out center;
""".trimIndent()

        val mediaType = "text/plain; charset=utf-8".toMediaType()
        val body = query.toRequestBody(mediaType)
        val req = Request.Builder()
            .url("https://overpass-api.de/api/interpreter")
            .post(body)
            .build()

        return try {
            val response = client.newCall(req).execute()
            if (!response.isSuccessful) return emptyList()
            val text = response.body?.string() ?: return emptyList()
            val jo = JSONObject(text)
            val elements = jo.optJSONArray("elements") ?: return emptyList()
            val points = mutableListOf<SafetyPoint>()
            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                val lat = el.optDouble("lat", el.optJSONObject("center")?.optDouble("lat") ?: Double.NaN)
                val lon = el.optDouble("lon", el.optJSONObject("center")?.optDouble("lon") ?: Double.NaN)
                if (!lat.isNaN() && !lon.isNaN()) {
                    points.add(SafetyPoint(lat, lon))
                }
            }
            points
        } catch (e: Exception) {
            emptyList()
        }
    }
}
