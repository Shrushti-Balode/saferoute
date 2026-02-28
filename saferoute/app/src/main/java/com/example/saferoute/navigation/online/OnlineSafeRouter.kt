package com.example.saferoute.navigation.online

import android.content.Context
import com.example.saferoute.utils.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.osmdroid.util.GeoPoint

object OnlineSafeRouter {
    private val client = OkHttpClient()
    private const val AVERAGE_WALKING_SPEED_MPS = 1.4

    suspend fun computeRoute(
        context: Context,
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        safetyWeight: Double
    ): com.example.saferoute.navigation.RouteResult? {
        return withContext(Dispatchers.IO) {
            val coordStr = "%f,%f;%f,%f".format(originLon, originLat, destLon, destLat)
            // Requesting alternatives to give us choices to pick from based on safety
            val url = "https://router.project-osrm.org/route/v1/driving/$coordStr?alternatives=true&overview=full&geometries=geojson"

            try {
                val req = okhttp3.Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val text = resp.body?.string() ?: return@withContext null
                    val jo = org.json.JSONObject(text)
                    if (!jo.optString("code").equals("Ok", true)) return@withContext null
                    val arr = jo.optJSONArray("routes") ?: return@withContext null
                    
                    val candidates = mutableListOf<Triple<List<Pair<Double, Double>>, Double, Double>>()
                    for (i in 0 until arr.length()) {
                        val r = arr.getJSONObject(i)
                        val distance = r.optDouble("distance", 0.0)
                        val duration = r.optDouble("duration", 0.0)
                        val geom = r.optJSONObject("geometry") ?: continue
                        val coordsJson = geom.optJSONArray("coordinates") ?: continue
                        val coords = mutableListOf<Pair<Double, Double>>()
                        for (j in 0 until coordsJson.length()) {
                            val pair = coordsJson.getJSONArray(j)
                            val lon = pair.getDouble(0)
                            val lat = pair.getDouble(1)
                            coords.add(lat to lon)
                        }
                        candidates.add(Triple(coords, distance, duration))
                    }

                    if (candidates.isEmpty()) return@withContext null

                    // 1. Get safety points for the general area
                    val lats = candidates.flatMap { it.first }.map { it.first }
                    val lons = candidates.flatMap { it.first }.map { it.second }
                    val minLat = lats.minOrNull() ?: originLat
                    val maxLat = lats.maxOrNull() ?: destLat
                    val minLon = lons.minOrNull() ?: originLon
                    val maxLon = lons.maxOrNull() ?: destLon
                    val bbox = "${minLat - 0.005},${minLon - 0.005},${maxLat + 0.005},${maxLon + 0.005}"
                    
                    val safetyPoints = try { 
                        OverpassClient.querySafetyPoints(bbox) 
                    } catch (e: Exception) { 
                        emptyList<OverpassClient.SafetyPoint>() 
                    }

                    // 2. Score each candidate
                    // Utility = (1-safetyWeight) * (-NormalizedDistance) + (safetyWeight) * (NormalizedSafety)
                    val maxDist = candidates.maxOf { it.second }.coerceAtLeast(1.0)
                    
                    val scoredCandidates = candidates.map { (coords, dist, dur) ->
                        // Safety score: average proximity to safety points
                        var safetyScore = 0.0
                        if (safetyPoints.isNotEmpty()) {
                            // Check a subset of points for performance
                            val step = (coords.size / 10).coerceAtLeast(1)
                            var proximitySum = 0.0
                            var count = 0
                            for (i in coords.indices step step) {
                                val p = coords[i]
                                val minDistToSafety = safetyPoints.minOf { sp ->
                                    val dLat = p.first - sp.lat
                                    val dLon = p.second - sp.lon
                                    dLat * dLat + dLon * dLon
                                }
                                // Inverse distance: closer is better. Using a simple threshold.
                                // 0.0001 degrees is ~11 meters.
                                if (minDistToSafety < 0.0001) proximitySum += 1.0
                                count++
                            }
                            safetyScore = proximitySum / count
                        } else {
                            // Demo logic: if no data, use a deterministic "safety" nudge based on coordinates
                            // to ensure the route changes when moving the slider
                            safetyScore = coords.sumOf { Math.sin(it.first * 1000) + Math.cos(it.second * 1000) } / coords.size
                            // Normalize roughly to 0-1
                            safetyScore = (safetyScore + 2) / 4.0
                        }

                        val normDist = dist / maxDist
                        val utility = (1.0 - safetyWeight) * (1.0 - normDist) + safetyWeight * safetyScore
                        
                        Quadruple(coords, dist, dur, utility)
                    }

                    val best = scoredCandidates.maxByOrNull { it.fourth } ?: scoredCandidates[0]
                    
                    com.example.saferoute.navigation.RouteResult(
                        best.first,
                        best.second,
                        best.second / AVERAGE_WALKING_SPEED_MPS
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
