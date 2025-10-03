package com.example.carbontracer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BadgeAdapter(private val badges: List<Badge>) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        holder.badgeName.text = badge.name
        holder.badgeDescription.text = badge.description
        holder.badgeIcon.setImageResource(badge.icon)

        if (badge.isEarned) {
            holder.badgeIcon.alpha = 1.0f
            holder.badgeName.alpha = 1.0f
            holder.badgeDescription.alpha = 1.0f
            holder.lockedIcon.visibility = View.GONE
        } else {
            holder.badgeIcon.alpha = 0.5f
            holder.badgeName.alpha = 0.5f
            holder.badgeDescription.alpha = 0.5f
            holder.lockedIcon.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = badges.size

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val badgeIcon: ImageView = itemView.findViewById(R.id.ivBadgeIcon)
        val badgeName: TextView = itemView.findViewById(R.id.tvBadgeName)
        val badgeDescription: TextView = itemView.findViewById(R.id.tvBadgeDescription)
        val lockedIcon: ImageView = itemView.findViewById(R.id.ivLocked)
    }
}