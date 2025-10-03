package com.example.carbontracer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SettingsAdapter(private val settings: List<Setting>, private val onItemClick: (Setting) -> Unit) :
    RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_setting, parent, false)
        return SettingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val setting = settings[position]
        holder.title.text = setting.title
        holder.description.text = setting.description
        holder.itemView.setOnClickListener { onItemClick(setting) }
    }

    override fun getItemCount() = settings.size

    class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvSettingTitle)
        val description: TextView = itemView.findViewById(R.id.tvSettingDescription)
    }
}