package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MediaItemEntity::class, SmartSensorEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MediaDashDatabase : RoomDatabase() {

    abstract fun mediaDao(): MediaDao
    abstract fun sensorDao(): SensorDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDashDatabase? = null

        fun getDatabase(context: Context): MediaDashDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediaDashDatabase::class.java,
                    "mediadash_hub.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
