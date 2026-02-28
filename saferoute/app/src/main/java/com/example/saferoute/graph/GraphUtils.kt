package com.example.saferoute.graph

import com.example.saferoute.utils.GeoUtils

object GraphUtils {
    fun findNearestNodeKey(nodesKeys: Collection<String>, lat: Double, lon: Double): String {
        return nodesKeys.minByOrNull { key ->
            val parts = key.split(":")
            val nodeLat = parts[0].toDouble()
            val nodeLon = parts[1].toDouble()
            GeoUtils.haversineDistMeters(lat, lon, nodeLat, nodeLon)
        }!!
    }
}
