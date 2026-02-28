package com.example.saferoute.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import com.example.saferoute.R

object SOSUtils {

    fun handleSOS(context: Context, navController: NavController, lat: Double, lon: Double) {
        val sharedPreferences = context.getSharedPreferences("saferoute_prefs", Context.MODE_PRIVATE)
        val contactNumber = sharedPreferences.getString("sos_contact_number", null)

        if (contactNumber.isNullOrBlank()) {
            AlertDialog.Builder(context)
                .setTitle("SOS Contact Not Set")
                .setMessage("Please set an emergency contact number in settings.")
                .setPositiveButton("Set Contact") { _, _ ->
                    try {
                        navController.navigate(R.id.action_mapFragment_to_sosContactsFragment)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Navigate to SOS Contacts manually", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Physically open the SMS app with the message pre-filled
            openSmsApp(context, contactNumber, lat, lon)
            promptEmergencyCall(context)
        }
    }

    fun sendAutomatedSOS(context: Context, lat: Double, lon: Double) {
        val sharedPreferences = context.getSharedPreferences("saferoute_prefs", Context.MODE_PRIVATE)
        val contactNumber = sharedPreferences.getString("sos_contact_number", "") ?: ""
        openSmsApp(context, contactNumber, lat, lon)
    }

    private fun openSmsApp(context: Context, phoneNumber: String, lat: Double, lon: Double) {
        val message = "🚨 SOS ALERT! I need help. My current location: https://maps.google.com/?q=$lat,$lon"
        
        // This Intent physically opens the Google Messages app
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
            Toast.makeText(context, "Opening Messages...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Fallback for some older emulators
            val backupIntent = Intent(Intent.ACTION_VIEW).apply {
                type = "vnd.android-dir/mms-sms"
                putExtra("address", phoneNumber)
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(backupIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open SMS app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun promptEmergencyCall(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Contact Emergency Services?")
            .setMessage("Would you like to call 100?")
            .setPositiveButton("Yes, Call") { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:100")
                context.startActivity(intent)
            }
            .setNegativeButton("No", null)
            .show()
    }
}
