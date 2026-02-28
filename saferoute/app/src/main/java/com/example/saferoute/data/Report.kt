package com.example.saferoute.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val synced: Boolean = false
)