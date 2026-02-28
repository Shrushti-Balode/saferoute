package com.example.saferoute.ui.safety

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.saferoute.R
import com.example.saferoute.data.Report
import com.example.saferoute.database.AppDatabase
import kotlinx.coroutines.launch

class ReportFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        val spinner: Spinner = view.findViewById(R.id.spinnerType)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.report_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        val notes: EditText = view.findViewById(R.id.etNotes)
        val saveButton: Button = view.findViewById(R.id.btnSaveReport)

        saveButton.setOnClickListener {
            val report = Report(
                type = spinner.selectedItem.toString(),
                description = notes.text.toString(),
                latitude = 18.5204, // Pune demo coords
                longitude = 73.8567,
                timestamp = System.currentTimeMillis()
            )
            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext()).appDao().insertReport(report)
                Toast.makeText(requireContext(), "Report saved", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        return view
    }
}

