package com.example.saferoute.ui.safety

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferoute.data.PoliceStation
import com.example.saferoute.database.AppDatabase
import kotlinx.coroutines.launch

class SafetyViewModel(application: Application) : AndroidViewModel(application) {

    private val appDao = AppDatabase.getDatabase(application).appDao()

    fun insertDummyPoliceData() {
        viewModelScope.launch {
            val dummyStations = listOf(
                PoliceStation(1, "Koregaon Park Police Station", 18.5361, 73.8935, "Ghorpadi, Pune"),
                PoliceStation(2, "Shivaji Nagar Police Station", 18.5293, 73.8483, "Shivaji Nagar, Pune"),
                PoliceStation(3, "Deccan Gymkhana Police Station", 18.5185, 73.8381, "Deccan Gymkhana, Pune")
            )
            appDao.insertPoliceStations(dummyStations)
        }
    }
}