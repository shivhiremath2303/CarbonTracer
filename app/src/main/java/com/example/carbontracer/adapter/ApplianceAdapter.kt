package com.example.carbontracer.adapter // Change to your package name

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.R
import com.example.carbontracer.model.Appliance // Import your model

class ApplianceAdapter(private var applianceList: List<Appliance>) :
    RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder>() {

    // This creates the view for each row
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appliance, parent, false)
        return ApplianceViewHolder(view)
    }

    // This binds the data from your appliance object to the TextViews
    override fun onBindViewHolder(holder: ApplianceViewHolder, position: Int) {
        val appliance = applianceList[position]
        holder.bind(appliance)
    }

    override fun getItemCount(): Int = applianceList.size

    // This is the "View Holder" class that holds the TextViews
    // YOUR CRASH WAS HAPPENING HERE
    class ApplianceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // This line will now find the ID from the XML above
        private val nameTextView: TextView = itemView.findViewById(R.id.text_appliance_name)

        // This line will also find the ID from the XML above
        private val usageTextView: TextView = itemView.findViewById(R.id.text_appliance_usage)

        fun bind(appliance: Appliance) {
            nameTextView.text = appliance.applianceName
            usageTextView.text = "Usage: ${appliance.dailyHoursUsed} hours per day"
        }
    }

    // This function is called by ElectricityActivity to update the data
    fun updateList(newList: List<Appliance>) {
        applianceList = newList
        notifyDataSetChanged() // Refreshes the list
    }
}