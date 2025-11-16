package com.example.carbontracer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.R
import com.example.carbontracer.model.Tip // Import your Tip model

class TipAdapter(private var tipList: List<Tip>) :
    RecyclerView.Adapter<TipAdapter.TipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tip, parent, false)
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        holder.bind(tipList[position])
    }

    override fun getItemCount(): Int = tipList.size

    class TipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.text_tip_title)
        private val descTextView: TextView = itemView.findViewById(R.id.text_tip_description)

        fun bind(tip: Tip) {
            titleTextView.text = tip.title
            descTextView.text = tip.description
        }
    }

    // Function to update the list (just like your ApplianceAdapter)
    fun updateList(newList: List<Tip>) {
        tipList = newList
        notifyDataSetChanged()
    }
}