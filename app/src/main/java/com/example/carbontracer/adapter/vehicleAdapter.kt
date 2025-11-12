package com.example.carbontracer.adapter



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.R
import com.example.carbontracer.model.Vehicle

class VehicleAdapter(private var vehicleList: List<Vehicle>) :
    RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicleList[position]
        holder.bind(vehicle)
    }

    override fun getItemCount(): Int = vehicleList.size

    fun updateList(newList: List<Vehicle>) {
        vehicleList = newList
        notifyDataSetChanged() // In a real app, use DiffUtil for efficiency
    }

    class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNickname: TextView = itemView.findViewById(R.id.tvVehicleNickname)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvVehicleDetails)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivVehicleIcon)

        fun bind(vehicle: Vehicle) {
            tvNickname.text = vehicle.nickname
            val details = "${vehicle.fuelType} - ${vehicle.efficiency} ${vehicle.efficiencyUnit}"
            tvDetails.text = details

            // Set icon based on type (optional but nice)
            when (vehicle.vehicleType) {
                "Car" -> ivIcon.setImageResource(R.drawable.ic_car)
                "Motorcycle" -> ivIcon.setImageResource(R.drawable.ic_motorcycle)
                else -> ivIcon.setImageResource(R.drawable.ic_car)
            }
        }
    }
}