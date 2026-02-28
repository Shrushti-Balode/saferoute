package com.example.saferoute.navigation

data class RouteResult(
    val points: List<Pair<Double, Double>>,
    val totalDistanceMeters: Double,
    val estimatedTimeSeconds: Double
)
