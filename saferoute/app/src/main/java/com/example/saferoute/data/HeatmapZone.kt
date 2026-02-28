package com.example.saferoute.data

data class HeatmapZone(
    val latitude: Double,
    val longitude: Double,
    val radius: Double, // in meters
    val safetyScore: Int // 0-100 (0=dangerous, 100=safe)
)
