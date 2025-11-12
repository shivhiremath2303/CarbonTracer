package com.example.carbontracer.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.R
import com.example.carbontracer.model.Appliance

class ApplianceAdapter(private var applianceList: List<Appliance>) :
    RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appliance, parent, false)
        return ApplianceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplianceViewHolder, position: Int) {
        val appliance = applianceList[position]
        holder.bind(appliance)
    }

    override fun getItemCount(): Int = applianceList.size

    fun updateList(newList: List<Appliance>) {
        applianceList = newList
        notifyDataSetChanged()
    }

    class ApplianceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvApplianceName)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvApplianceDetails)

        fun bind(appliance: Appliance) {
            val name = "${appliance.applianceName} (x${appliance.applianceCount})"
            val details = "${appliance.dailyHoursUsed} hours/day (${appliance.wattageUsed}W)"

            tvName.text = name
            tvDetails.text = details
        }
    }
}