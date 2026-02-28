package com.example.saferoute.ui.safety

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R

class SosContactsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sos_contacts, container, false)

        val etContactName = view.findViewById<EditText>(R.id.etContactName)
        val etContactNumber = view.findViewById<EditText>(R.id.etContactNumber)
        val btnSaveContact = view.findViewById<Button>(R.id.btnSaveContact)

        val sharedPreferences = requireActivity().getSharedPreferences("saferoute_prefs", Context.MODE_PRIVATE)

        etContactName.setText(sharedPreferences.getString("sos_contact_name", ""))
        etContactNumber.setText(sharedPreferences.getString("sos_contact_number", ""))

        btnSaveContact.setOnClickListener {
            val name = etContactName.text.toString()
            val number = etContactNumber.text.toString()

            if (name.isNotBlank() && number.isNotBlank()) {
                with(sharedPreferences.edit()) {
                    putString("sos_contact_name", name)
                    putString("sos_contact_number", number)
                    apply()
                }
                Toast.makeText(requireContext(), "SOS contact saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Please enter a name and number", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
