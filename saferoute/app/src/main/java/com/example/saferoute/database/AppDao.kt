package com.example.saferoute.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.saferoute.data.GraphEdge
import com.example.saferoute.data.PickupSpot
import com.example.saferoute.data.PoiFts
import com.example.saferoute.data.PoliceStation
import com.example.saferoute.data.Report
import com.example.saferoute.data.StreetLight

@Dao
interface AppDao {
    // Police Stations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoliceStations(stations: List<PoliceStation>)

    @Query("SELECT * FROM police_stations")
    suspend fun getAllPoliceStations(): List<PoliceStation>

    // Reports
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report)

    @Query("SELECT * FROM reports WHERE synced = 0")
    suspend fun getUnsyncedReports(): List<Report>

    // Pickup Spots
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPickupSpots(spots: List<PickupSpot>)

    @Query("SELECT * FROM pickup_spots")
    suspend fun getAllPickupSpots(): List<PickupSpot>

    // Street Lights
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreetLights(lights: List<StreetLight>)

    @Query("SELECT * FROM street_lights")
    suspend fun getAllStreetLights(): List<StreetLight>

    // Graph Edges
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphEdges(edges: List<GraphEdge>)

    @Query("SELECT * FROM graph_edges")
    suspend fun getAllGraphEdges(): List<GraphEdge>

    // POI FTS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoisFts(pois: List<PoiFts>)

    @Query("SELECT * FROM poi_fts WHERE name MATCH :query")
    suspend fun searchPoiFts(query: String): List<PoiFts>
    
    @Query("SELECT * FROM poi_fts WHERE type = 'preset'")
    suspend fun getPresetDestinations(): List<PoiFts>
}
