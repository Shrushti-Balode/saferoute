package com.example.saferoute.graph

import com.example.saferoute.data.GraphEdge

data class EdgeNeighbor(
    val toKey: String,
    val length: Double,
    val safety: Double,
    val toLat: Double,
    val toLon: Double
)

object GraphBuilder {
    fun buildAdjacency(edges: List<GraphEdge>): Map<String, List<EdgeNeighbor>> {
        val adjacencyMap = mutableMapOf<String, MutableList<EdgeNeighbor>>()

        for (edge in edges) {
            val fromKey = "${edge.fromNodeLat}:${edge.fromNodeLon}"
            val toKey = "${edge.toNodeLat}:${edge.toNodeLon}"

            adjacencyMap.computeIfAbsent(fromKey) { mutableListOf() }.add(
                EdgeNeighbor(toKey, edge.lengthMeters, edge.safetyCost, edge.toNodeLat, edge.toNodeLon)
            )

            // Assuming an undirected graph for routing
            adjacencyMap.computeIfAbsent(toKey) { mutableListOf() }.add(
                EdgeNeighbor(fromKey, edge.lengthMeters, edge.safetyCost, edge.fromNodeLat, edge.fromNodeLon)
            )
        }
        return adjacencyMap
    }
}
