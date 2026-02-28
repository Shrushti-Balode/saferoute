package com.example.saferoute.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "graph_edges")
data class GraphEdge(
    @PrimaryKey val edgeId: Int,
    val fromNodeLat: Double,
    val fromNodeLon: Double,
    val toNodeLat: Double,
    val toNodeLon: Double,
    val lengthMeters: Double,
    val safetyCost: Double
)
