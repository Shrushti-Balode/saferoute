package com.example.saferoute.utils

import com.example.saferoute.data.HeatmapZone
import org.osmdroid.util.GeoPoint
import kotlin.random.Random

object HeatmapUtils {

    fun generateRandomHeatmap(centerLat: Double, centerLon: Double, numZones: Int, minRadius: Double, maxRadius: Double): List<HeatmapZone> {
        val zones = mutableListOf<HeatmapZone>()
        for (i in 0 until numZones) {
            var validPlacement = false
            var newZone: HeatmapZone? = null

            while (!validPlacement) {
                val lat = centerLat + (Random.nextDouble() - 0.5) * 0.1
                val lon = centerLon + (Random.nextDouble() - 0.5) * 0.1
                val radius = Random.nextDouble(minRadius, maxRadius)
                val safetyScore = Random.nextInt(0, 101)
                newZone = HeatmapZone(lat, lon, radius, safetyScore)

                var overlaps = false
                for (existingZone in zones) {
                    val distance = GeoPoint(newZone.latitude, newZone.longitude).distanceToAsDouble(GeoPoint(existingZone.latitude, existingZone.longitude))
                    if (distance < newZone.radius + existingZone.radius) {
                        overlaps = true
                        break
                    }
                }
                if (!overlaps) {
                    validPlacement = true
                }
            }
            newZone?.let { zones.add(it) }
        }
        return zones
    }
}
