package com.example.data.repository

import com.example.data.local.MediaDao
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaSource
import com.example.data.local.MediaType
import com.example.service.MediaScannerService
import kotlinx.coroutines.flow.Flow

class MediaRepository(
    private val mediaDao: MediaDao,
    private val scannerService: MediaScannerService
) {

    val allMedia: Flow<List<MediaItemEntity>> = mediaDao.getAllMedia()
    val resumeMedia: Flow<List<MediaItemEntity>> = mediaDao.getResumeMedia()

    fun getMediaByType(type: MediaType): Flow<List<MediaItemEntity>> = mediaDao.getMediaByType(type)

    fun getMediaBySource(source: MediaSource): Flow<List<MediaItemEntity>> = mediaDao.getMediaBySource(source)

    suspend fun syncMediaLibrary() {
        val count = mediaDao.getMediaCount()
        if (count == 0) {
            val scanned = scannerService.scanHybridSources()
            mediaDao.insertAll(scanned)
        }
    }

    suspend fun updateProgress(mediaId: Long, positionMs: Long) {
        mediaDao.updatePlaybackPosition(mediaId, positionMs)
    }

    suspend fun addMediaItem(item: MediaItemEntity): Long {
        return mediaDao.insert(item)
    }

    suspend fun refreshScan() {
        val freshItems = scannerService.scanHybridSources()
        mediaDao.insertAll(freshItems)
    }
}
