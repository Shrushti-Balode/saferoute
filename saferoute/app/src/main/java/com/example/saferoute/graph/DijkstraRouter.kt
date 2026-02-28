package com.example.saferoute.graph

import java.util.PriorityQueue

object DijkstraRouter {

    /**
     * Cost is a weighted sum of normalized length and safety cost.
     * alpha = 0.0 -> Speed priority (minimize length)
     * alpha = 1.0 -> Safety priority (minimize safety cost)
     */
    fun shortestPath(
        adj: Map<String, List<EdgeNeighbor>>,
        startKey: String,
        goalKey: String,
        alpha: Double
    ): List<String>? {
        val dist = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
        val prev = mutableMapOf<String, String?>()
        val pq = PriorityQueue<Pair<String, Double>>(compareBy { it.second })

        val maxLength = adj.values.flatten().maxOfOrNull { it.length } ?: 1.0

        dist[startKey] = 0.0
        pq.add(startKey to 0.0)

        while (pq.isNotEmpty()) {
            val (uKey, uDist) = pq.poll()

            if (uDist > dist.getValue(uKey)) {
                continue
            }

            if (uKey == goalKey) {
                val path = mutableListOf<String>()
                var current: String? = goalKey
                while (current != null) {
                    path.add(0, current)
                    current = prev[current]
                }
                return path
            }

            adj[uKey]?.forEach { neighbor ->
                val normalizedLength = neighbor.length / maxLength
                // alpha = 1.0 should prioritize safety cost, 0.0 should prioritize length
                val cost = (1.0 - alpha) * normalizedLength + alpha * neighbor.safety
                val newDist = uDist + cost

                if (newDist < dist.getValue(neighbor.toKey)) {
                    dist[neighbor.toKey] = newDist
                    prev[neighbor.toKey] = uKey
                    pq.add(neighbor.toKey to newDist)
                }
            }
        }
        return null // No path found
    }
}
