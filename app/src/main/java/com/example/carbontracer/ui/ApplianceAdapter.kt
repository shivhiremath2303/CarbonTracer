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
        val appliance = appliances[position]
        holder.tvApplianceName.text = appliance.name
        holder.tvApplianceModel.text = appliance.model
    }

    override fun getItemCount() = appliances.size

    class ApplianceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvApplianceName: TextView = itemView.findViewById(R.id.tvApplianceName)
        val tvApplianceModel: TextView = itemView.findViewById(R.id.tvApplianceModel)
    }
}
