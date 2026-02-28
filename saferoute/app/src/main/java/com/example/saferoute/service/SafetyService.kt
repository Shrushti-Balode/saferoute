package com.example.saferoute.service

import android.content.Context
import com.example.saferoute.data.PickupSpot
import com.example.saferoute.data.PoliceStation
import com.example.saferoute.database.AppDatabase
import com.example.saferoute.utils.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SafetyService {
    suspend fun findNearestPolice(context: Context, lat: Double, lon: Double): Pair<PoliceStation, Double>? {
        return withContext(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).appDao()
            val stations = dao.getAllPoliceStations()
            if (stations.isNotEmpty()) {
                stations.map { it to GeoUtils.haversineDistMeters(lat, lon, it.latitude, it.longitude) }
                    .minByOrNull { it.second }
            } else {
                null
            }
        }
    }

    suspend fun findNearestPickup(context: Context, lat: Double, lon: Double): Pair<PickupSpot, Double>? {
        return withContext(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).appDao()
            val spots = dao.getAllPickupSpots()
            if (spots.isNotEmpty()) {
                spots.map { it to GeoUtils.haversineDistMeters(lat, lon, it.latitude, it.longitude) }
                    .minByOrNull { it.second }
            } else {
                null
            }
        }
    }
}