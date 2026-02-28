package com.example.saferoute.data

import android.content.Context
import android.util.Log
import com.example.saferoute.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object SeedLoader {
    suspend fun seedIfNeeded(context: Context) {
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).appDao()
            try {
                // For this demo, we re-seed on every launch.
                // OnConflictStrategy.REPLACE will handle duplicates.

                val poiFts = mutableListOf<PoiFts>()

                // Seed Police Stations
                val policeStations = mutableListOf<PoliceStation>()
                context.assets.open("police_stations.csv").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readLine() // skip header
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val tokens = line!!.split(",")
                            val station = PoliceStation(tokens[0].toInt(), tokens[1], tokens[2].toDouble(), tokens[3].toDouble(), tokens[4])
                            policeStations.add(station)
                            poiFts.add(PoiFts(station.id, station.name, "police", station.latitude, station.longitude))
                        }
                    }
                }
                dao.insertPoliceStations(policeStations)

                // Seed Pickup Spots
                val pickupSpots = mutableListOf<PickupSpot>()
                context.assets.open("pickup_spots.csv").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readLine() // skip header
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val tokens = line!!.split(",")
                            val spot = PickupSpot(tokens[0].toInt(), tokens[1], tokens[2].toDouble(), tokens[3].toDouble(), tokens[4])
                            pickupSpots.add(spot)
                            poiFts.add(PoiFts(spot.id, spot.name, "pickup", spot.latitude, spot.longitude))
                        }
                    }
                }
                dao.insertPickupSpots(pickupSpots)

                // Seed Preset Destinations
                context.assets.open("preset_destinations.csv").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readLine() // skip header
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val tokens = line!!.split(",")
                            poiFts.add(PoiFts(tokens[0].toInt(), tokens[1], "preset", tokens[2].toDouble(), tokens[3].toDouble()))
                        }
                    }
                }
                
                dao.insertPoisFts(poiFts)

                // Seed Graph Edges
                val graphEdges = mutableListOf<GraphEdge>()
                context.assets.open("graph_edges.csv").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readLine() // skip header
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val tokens = line!!.split(",")
                            try {
                                val edge = GraphEdge(
                                    tokens[0].toInt(),
                                    tokens[1].toDouble(),
                                    tokens[2].toDouble(),
                                    tokens[3].toDouble(),
                                    tokens[4].toDouble(),
                                    tokens[5].toDouble(),
                                    tokens[6].toDouble()
                                )
                                graphEdges.add(edge)
                            } catch (_: Exception) {
                                // ignore malformed lines
                            }
                        }
                    }
                }
                if (graphEdges.isNotEmpty()) dao.insertGraphEdges(graphEdges)

            } catch (e: Exception) {
                Log.e("SeedLoader", "Error seeding database", e)
            }
        }
    }
}