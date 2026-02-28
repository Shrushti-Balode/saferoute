package com.example.saferoute

import android.content.Context
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration
import java.io.File

object AppConfig {
    fun configureOsmdroid(context: Context) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
        try {
            val tileCache = File("/sdcard/osmdroid_tiles/")
            if (tileCache.exists()) {
                Configuration.getInstance().osmdroidTileCache = tileCache
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}