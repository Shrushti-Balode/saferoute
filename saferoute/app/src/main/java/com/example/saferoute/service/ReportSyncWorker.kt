package com.example.saferoute.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.saferoute.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(applicationContext).appDao()
            val unsynced = dao.getUnsyncedReports()
            // In a real app, you would sync these to a server.
            // For this demo, we'll just mark them as synced.
            unsynced.forEach { 
                dao.insertReport(it.copy(synced = true))
            }
            Result.success()
        }
    }
}