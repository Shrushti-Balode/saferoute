package com.example.saferoute.data

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "poi_fts")
@Fts4
data class PoiFts(
    val poiId: Int,
    val name: String,
    val type: String,
    val lat: Double,
    val lon: Double
)
