package com.example.saferoute.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "street_lights")
data class StreetLight(
    @PrimaryKey val id: Int,
    val lat: Double,
    val lon: Double,
    val lit: Boolean
)