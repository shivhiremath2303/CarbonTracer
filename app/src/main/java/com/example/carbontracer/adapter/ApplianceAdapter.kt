package com.example.carbontracer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.R
import com.example.carbontracer.model.Appliance

class ApplianceAdapter(
    private var applianceList: List<Appliance>,
    private val onEdit: (Appliance) -> Unit,
    private val onDelete: (Appliance) -> Unit
) : RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appliance, parent, false)
        return ApplianceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplianceViewHolder, position: Int) {
        val appliance = applianceList[position]
        holder.bind(appliance, onEdit, onDelete)
    }

    override fun getItemCount(): Int = applianceList.size

    class ApplianceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.text_appliance_name)
        private val usageTextView: TextView = itemView.findViewById(R.id.text_appliance_usage)
        private val editButton: ImageView = itemView.findViewById(R.id.ivEdit)
        private val deleteButton: ImageView = itemView.findViewById(R.id.ivDelete)

        fun bind(appliance: Appliance, onEdit: (Appliance) -> Unit, onDelete: (Appliance) -> Unit) {
            nameTextView.text = "${appliance.applianceCount}x ${appliance.applianceName}"

            val kwh = (appliance.wattageUsed * appliance.dailyHoursUsed * 30.0) / 1000.0
            val co2 = kwh * 0.4 // Simplified CO2 calculation
            usageTextView.text = "%.1f kg CO2/month".format(co2)

            editButton.setOnClickListener { onEdit(appliance) }
            deleteButton.setOnClickListener { onDelete(appliance) }
        }
    }

    fun updateList(newList: List<Appliance>) {
        applianceList = newList
        notifyDataSetChanged()
    }
}
