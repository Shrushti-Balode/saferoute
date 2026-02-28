package com.example.saferoute.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.saferoute.data.GraphEdge
import com.example.saferoute.data.PickupSpot
import com.example.saferoute.data.PoiFts
import com.example.saferoute.data.PoliceStation
import com.example.saferoute.data.Report
import com.example.saferoute.data.StreetLight

@Database(entities = [PoliceStation::class, Report::class, PickupSpot::class, StreetLight::class, GraphEdge::class, PoiFts::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}