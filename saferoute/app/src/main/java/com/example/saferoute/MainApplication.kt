package com.example.saferoute

import android.app.Application
import com.example.saferoute.data.SeedLoader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalScope.launch {
            SeedLoader.seedIfNeeded(this@MainApplication)
        }
    }
}
