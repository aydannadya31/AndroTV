package com.example.data.repository

import com.example.data.local.SensorDao
import com.example.data.local.SmartSensorEntity
import com.example.service.MqttNetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SmartHomeRepository(
    private val sensorDao: SensorDao,
    private val mqttService: MqttNetworkService
) {

    val allSensors: Flow<List<SmartSensorEntity>> = sensorDao.getAllSensors()

    suspend fun initializeSensors() {
        if (sensorDao.getSensorCount() == 0) {
            val initial = mqttService.getInitialSensors()
            sensorDao.insertOrUpdateAll(initial)
        }
    }

    fun startMqttSubscription(scope: CoroutineScope) {
        mqttService.startListening(scope)
        scope.launch(Dispatchers.IO) {
            mqttService.incomingMessages.collect { msg ->
                when (msg.topic) {
                    "homeassistant/sensor/living_room/temperature" -> {
                        sensorDao.updateSensorState("sensor.living_room_temp", msg.payload, true)
                    }
                    "homeassistant/sensor/living_room/humidity" -> {
                        sensorDao.updateSensorState("sensor.living_room_humidity", msg.payload, true)
                    }
                    "homeassistant/sensor/home/power_usage" -> {
                        sensorDao.updateSensorState("sensor.total_power", msg.payload, true)
                    }
                }
            }
        }
    }

    suspend fun toggleSwitch(entityId: String, currentActive: Boolean) {
        val newState = if (currentActive) "KAPALI" else "AÇIK"
        sensorDao.updateSensorState(entityId, newState, !currentActive)
    }

    suspend fun triggerCinemaMode() {
        sensorDao.updateSensorState("switch.cinema_lights", "AÇIK (%20)", true)
    }
}
