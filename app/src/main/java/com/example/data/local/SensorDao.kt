package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {

    @Query("SELECT * FROM smart_sensors ORDER BY entityId ASC")
    fun getAllSensors(): Flow<List<SmartSensorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(sensors: List<SmartSensorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(sensor: SmartSensorEntity)

    @Query("UPDATE smart_sensors SET stateValue = :stateValue, isActive = :isActive, lastUpdated = :timestamp WHERE entityId = :entityId")
    suspend fun updateSensorState(entityId: String, stateValue: String, isActive: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM smart_sensors")
    suspend fun getSensorCount(): Int
}
