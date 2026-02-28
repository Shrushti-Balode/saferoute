package com.example.saferoute.navigation

import android.content.Context
import com.example.saferoute.database.AppDatabase
import com.example.saferoute.graph.DijkstraRouter
import com.example.saferoute.graph.GraphBuilder
import com.example.saferoute.graph.GraphUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.saferoute.navigation.online.OnlineSafeRouter

object NavigationManager {

    private const val AVERAGE_WALKING_SPEED_MPS = 1.4 // meters per second

    suspend fun computeRoute(
        context: Context,
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        alpha: Double
    ): RouteResult? {
        // Try online safe routing first (OSRM + Overpass). If it fails, fall back to local graph Dijkstra.
        val online = try {
            val result = OnlineSafeRouter.computeRoute(context, originLat, originLon, destLat, destLon, alpha)
            android.util.Log.d("NavigationManager", "online router returned ${result?.points?.size ?: 0} points (null=${result==null})")
            result
        } catch (e: Exception) {
            android.util.Log.w("NavigationManager", "online router threw", e)
            null
        }

        if (online != null) return online
        android.util.Log.d("NavigationManager", "online routing failed, falling back to offline graph")

        return withContext(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).appDao()
            val edges = dao.getAllGraphEdges()
            android.util.Log.d("NavigationManager", "offline edges count = ${edges.size}")
            if (edges.isEmpty()) return@withContext null

            val adjacencyMap = GraphBuilder.buildAdjacency(edges)
            val allNodeKeys = adjacencyMap.keys
            android.util.Log.d("NavigationManager", "adjacency nodes = ${allNodeKeys.size}")

            if (allNodeKeys.isEmpty()) return@withContext null

            val startKey = GraphUtils.findNearestNodeKey(allNodeKeys, originLat, originLon)
            val goalKey = GraphUtils.findNearestNodeKey(allNodeKeys, destLat, destLon)
            android.util.Log.d("NavigationManager", "startKey=$startKey goalKey=$goalKey")

            val pathKeys = DijkstraRouter.shortestPath(adjacencyMap, startKey, goalKey, alpha)
            android.util.Log.d("NavigationManager", "pathKeys result size=${pathKeys?.size}")

            if (pathKeys != null && pathKeys.size > 1) {
                val points = pathKeys.map {
                    val parts = it.split(":")
                    parts[0].toDouble() to parts[1].toDouble()
                }
                
                var totalDistance = 0.0
                for (i in 0 until pathKeys.size - 1) {
                    val currentKey = pathKeys[i]
                    val nextKey = pathKeys[i+1]
                    val neighbor = adjacencyMap[currentKey]?.find { it.toKey == nextKey }
                    if (neighbor != null) {
                        totalDistance += neighbor.length
                    }
                }

                val estimatedTime = totalDistance / AVERAGE_WALKING_SPEED_MPS
                RouteResult(points, totalDistance, estimatedTime)
            } else {
                null
            }
        }
    }
}
