package com.example.carbontracer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appliances")
data class Appliance(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val model: String,
    val powerConsumption: Double // in Watts
)
