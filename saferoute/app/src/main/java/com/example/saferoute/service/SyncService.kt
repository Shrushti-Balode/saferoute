package com.example.saferoute.service

import android.content.Context
import com.example.saferoute.data.Report
import com.example.saferoute.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncService {
    suspend fun getUnsyncedReports(context: Context): List<Report> {
        return withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).appDao().getUnsyncedReports()
        }
    }
}