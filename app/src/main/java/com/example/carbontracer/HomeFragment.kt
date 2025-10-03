package com.example.carbontracer

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnElectricity = view.findViewById<Button>(R.id.button_electricity)
        btnElectricity.setOnClickListener {
            val intent = Intent(activity, ElectricityActivity::class.java)
            startActivity(intent)
        }

        val btnTransport = view.findViewById<Button>(R.id.button_transport)
        btnTransport.setOnClickListener {
            val intent = Intent(activity, TransportActivity::class.java)
            startActivity(intent)
        }
    }
}