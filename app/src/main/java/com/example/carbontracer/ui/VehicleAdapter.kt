package com.example.carbontracer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.R
import com.example.carbontracer.model.Vehicle

class VehicleAdapter(
    private var vehicles: List<Vehicle>,
    private val onEdit: (Vehicle) -> Unit,
    private val onDelete: (Vehicle) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.bind(vehicle)
    }

    override fun getItemCount() = vehicles.size

    fun updateList(newList: List<Vehicle>) {
        vehicles = newList
        notifyDataSetChanged()
    }

    inner class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvVehicleNickname: TextView = itemView.findViewById(R.id.tvVehicleNickname)
        private val tvVehicleDetails: TextView = itemView.findViewById(R.id.tvVehicleDetails)
        private val ivVehicleIcon: ImageView = itemView.findViewById(R.id.ivVehicleIcon)
        private val ivEdit: ImageView = itemView.findViewById(R.id.ivEditVehicle)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDeleteVehicle)

        fun bind(vehicle: Vehicle) {
            tvVehicleNickname.text = vehicle.nickname

            val details = mutableListOf<String>()
            if (vehicle.efficiency > 0) {
                details.add("${vehicle.efficiency} ${vehicle.efficiencyUnit}")
            }
            if (vehicle.dieselLiters > 0) {
                details.add("${vehicle.dieselLiters}L/month (Diesel)")
            }

            tvVehicleDetails.text = if (details.isNotEmpty()) details.joinToString(" - ") else "No consumption data"


            when (vehicle.vehicleType) {
                "Car" -> ivVehicleIcon.setImageResource(R.drawable.ic_car)
                "Motorcycle" -> ivVehicleIcon.setImageResource(R.drawable.ic_motorcycle)
                else -> ivVehicleIcon.setImageResource(R.drawable.ic_car) // Default icon
            }

            ivEdit.setOnClickListener { onEdit(vehicle) }
            ivDelete.setOnClickListener { onDelete(vehicle) }
        }
    }
}
