package com.example.saferoute.ui.safety

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentSafetyBinding
import com.example.saferoute.service.LocationForegroundService
import com.example.saferoute.utils.SOSUtils

class SafetyFragment : Fragment() {

    private var _binding: FragmentSafetyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SafetyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSafetyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnInsertPolice.setOnClickListener {
            viewModel.insertDummyPoliceData()
        }

        binding.btnSOS.setOnClickListener {
            SOSUtils.handleSOS(requireContext(), findNavController(), 18.5308, 73.8475)
        }

        binding.btnSOS.setOnLongClickListener {
            try {
                findNavController().navigate(R.id.action_safetyFragment_to_sosContactsFragment)
                Toast.makeText(requireContext(), "Opening SOS Settings...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not open SOS settings", Toast.LENGTH_SHORT).show()
            }
            true
        }

        binding.btnStartService.setOnClickListener {
            val intent = Intent(requireContext(), LocationForegroundService::class.java)
            ContextCompat.startForegroundService(requireContext(), intent)
        }

        binding.btnOpenMap.setOnClickListener {
            findNavController().navigate(R.id.action_safetyFragment_to_mapFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
