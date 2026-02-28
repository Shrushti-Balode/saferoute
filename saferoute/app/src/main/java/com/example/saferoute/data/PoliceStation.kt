package com.example.saferoute.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "police_stations")
data class PoliceStation(
    @PrimaryKey val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)