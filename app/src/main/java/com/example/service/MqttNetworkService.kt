package com.example.service

import com.example.data.local.SensorType
import com.example.data.local.SmartSensorEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MqttMessage(
    val topic: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MqttNetworkService {

    private val _incomingMessages = MutableSharedFlow<MqttMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<MqttMessage> = _incomingMessages

    fun startListening(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            // Canlı sensör telemetrisi ve Home Assistant MQTT simülasyonu
            var temp = 22.4
            var humidity = 48
            var power = 345

            while (isActive) {
                delay(8000)
                temp = (22.0 + Math.random() * 1.5)
                humidity = (47 + (Math.random() * 5).toInt())
                power = (320 + (Math.random() * 60).toInt())

                _incomingMessages.emit(
                    MqttMessage(
                        topic = "homeassistant/sensor/living_room/temperature",
                        payload = String.format(java.util.Locale.US, "%.1f", temp)
                    )
                )
                _incomingMessages.emit(
                    MqttMessage(
                        topic = "homeassistant/sensor/living_room/humidity",
                        payload = "$humidity"
                    )
                )
                _incomingMessages.emit(
                    MqttMessage(
                        topic = "homeassistant/sensor/home/power_usage",
                        payload = "$power"
                    )
                )
            }
        }
    }

    fun getInitialSensors(): List<SmartSensorEntity> {
        return listOf(
            SmartSensorEntity(
                entityId = "sensor.living_room_temp",
                name = "Oturma Odası",
                stateValue = "22.8",
                unit = "°C",
                type = SensorType.TEMPERATURE,
                location = "Salon",
                isActive = true,
                iconKey = "temperature"
            ),
            SmartSensorEntity(
                entityId = "sensor.living_room_humidity",
                name = "Nem Seviyesi",
                stateValue = "49",
                unit = "%",
                type = SensorType.HUMIDITY,
                location = "Salon",
                isActive = true,
                iconKey = "water_drop"
            ),
            SmartSensorEntity(
                entityId = "switch.cinema_lights",
                name = "Sinema Ambiyansı",
                stateValue = "AÇIK",
                unit = "",
                type = SensorType.LIGHT_SWITCH,
                location = "TV Ünitesi",
                isActive = true,
                iconKey = "lightbulb"
            ),
            SmartSensorEntity(
                entityId = "sensor.total_power",
                name = "Anlık Tüketim",
                stateValue = "360",
                unit = "W",
                type = SensorType.POWER_METER,
                location = "Ana Hat",
                isActive = true,
                iconKey = "bolt"
            ),
            SmartSensorEntity(
                entityId = "binary_sensor.hall_motion",
                name = "Hareket Sensörü",
                stateValue = "Normal",
                unit = "",
                type = SensorType.SECURITY_MOTION,
                location = "Koridor",
                isActive = false,
                iconKey = "security"
            ),
            SmartSensorEntity(
                entityId = "sensor.air_quality",
                name = "Hava Kalitesi",
                stateValue = "İyi (AQI 28)",
                unit = "",
                type = SensorType.AIR_QUALITY,
                location = "İç Mekan",
                isActive = true,
                iconKey = "air"
            )
        )
    }
}
