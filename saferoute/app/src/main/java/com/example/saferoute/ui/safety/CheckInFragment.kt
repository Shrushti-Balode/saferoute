package com.example.saferoute.ui.safety

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.saferoute.R
import com.example.saferoute.utils.SOSUtils

class CheckInFragment : Fragment() {

    private lateinit var timer: CountDownTimer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checkin, container, false)

        val etMinutes: EditText = view.findViewById(R.id.etMinutes)
        val btnStart: Button = view.findViewById(R.id.btnStartCheckIn)
        val btnSafe: Button = view.findViewById(R.id.btnImSafe)
        val tvCountdown: TextView = view.findViewById(R.id.tvCountdown)

        btnSafe.visibility = View.GONE

        btnStart.setOnClickListener {
            val minutes = etMinutes.text.toString().toLongOrNull() ?: 0
            if (minutes > 0) {
                btnStart.isEnabled = false
                btnSafe.visibility = View.VISIBLE

                timer = object : CountDownTimer(minutes * 60 * 1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val seconds = millisUntilFinished / 1000
                        tvCountdown.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
                    }

                    override fun onFinish() {
                        tvCountdown.text = "Missed check-in!"
                        // Automatically sends alert to saved contact or opens SMS app
                        SOSUtils.sendAutomatedSOS(requireContext(), 18.5204, 73.8567) 
                        btnStart.isEnabled = true
                        btnSafe.visibility = View.GONE
                    }
                }.start()
            }
        }

        btnSafe.setOnClickListener {
            if (::timer.isInitialized) {
                timer.cancel()
            }
            tvCountdown.text = "Checked in safely!"
            btnStart.isEnabled = true
            btnSafe.visibility = View.GONE
        }

        return view
    }
}
