package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SensorType {
    TEMPERATURE,
    HUMIDITY,
    LIGHT_SWITCH,
    POWER_METER,
    SECURITY_MOTION,
    AIR_QUALITY
}

@Entity(tableName = "smart_sensors")
data class SmartSensorEntity(
    @PrimaryKey
    val entityId: String,
    val name: String,
    val stateValue: String,
    val unit: String = "",
    val type: SensorType = SensorType.TEMPERATURE,
    val location: String = "Oturma Odası",
    val isActive: Boolean = false,
    val iconKey: String = "thermometer",
    val lastUpdated: Long = System.currentTimeMillis()
)
