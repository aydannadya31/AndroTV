package com.example.player

import com.example.data.local.MediaItemEntity

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    COMPLETED
}

data class MediaPlayerState(
    val activeItem: MediaItemEntity? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    val audioTrack: String = "Dolby Atmos 7.1 (Tr)",
    val subtitleTrack: String = "Türkçe (Varsayılan)"
) {
    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING

    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    fun formattedTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
