package com.example.carbontracer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.R
import com.example.carbontracer.model.Appliance

class ApplianceAdapter(private val appliances: List<Appliance>) : RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appliance, parent, false)
        return ApplianceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplianceViewHolder, position: Int) {
        val appliance = appliances[position] // Make sure this is applianceList

        // Call the bind function you just created
        holder.bind(appliance)
    }
    override fun getItemCount() = appliances.size

    class ApplianceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // 1. Correct the IDs to match your item_appliance.xml
        private val tvName: TextView = itemView.findViewById(R.id.tvApplianceName)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvApplianceDetails) // <-- This was tvApplianceModel

        // 2. Add the bind function to fix the build errors
        fun bind(appliance: Appliance) {

            // This uses the correct properties from Appliance.kt
            val name = "${appliance.applianceName} (x${appliance.applianceCount})"
            val details = "${appliance.dailyHoursUsed} hours/day (${appliance.wattageUsed}W)"

            tvName.text = name
            tvDetails.text = details
        }
    }
}
