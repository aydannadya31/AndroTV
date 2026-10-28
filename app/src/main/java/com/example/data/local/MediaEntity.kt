package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    MOVIE,
    SERIES,
    MUSIC,
    DOCUMENTARY,
    PHOTO
}

enum class MediaSource {
    LOCAL_STORAGE,
    USB_DRIVE,
    SMB_SHARE,
    NAS_SERVER,
    PHONE_CAST
}

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subtitle: String,
    val durationMs: Long,
    val lastPositionMs: Long = 0L,
    val type: MediaType = MediaType.MOVIE,
    val source: MediaSource = MediaSource.LOCAL_STORAGE,
    val filePathOrUri: String,
    val resolution: String = "4K HDR",
    val rating: String = "8.4",
    val year: String = "2024",
    val bannerColor: Long = 0xFF10B981,
    val isResumeCandidate: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
