package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.PhoneActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Telefon ekranı kapandığında veya kilit ekranına geçtiğinde
 * TCP soket ve canlı medya yayın akışının kesilmesini engelleyen
 * Partial WakeLock ve WifiLock destekli Foreground Service.
 */
class PhoneStreamingForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        const val CHANNEL_ID = "mediadash_cast_stream_channel"
        const val NOTIFICATION_ID = 4091

        const val ACTION_START_STREAM = "ACTION_START_STREAM"
        const val ACTION_STOP_STREAM = "ACTION_STOP_STREAM"
        const val EXTRA_MEDIA_TITLE = "EXTRA_MEDIA_TITLE"
        const val EXTRA_TV_NAME = "EXTRA_TV_NAME"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        private val _currentStreamingTitle = MutableStateFlow<String?>(null)
        val currentStreamingTitle: StateFlow<String?> = _currentStreamingTitle

        fun startStreaming(context: Context, mediaTitle: String, tvName: String = "MediaDash TV") {
            val intent = Intent(context, PhoneStreamingForegroundService::class.java).apply {
                action = ACTION_START_STREAM
                putExtra(EXTRA_MEDIA_TITLE, mediaTitle)
                putExtra(EXTRA_TV_NAME, tvName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopStreaming(context: Context) {
            val intent = Intent(context, PhoneStreamingForegroundService::class.java).apply {
                action = ACTION_STOP_STREAM
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_STREAM -> {
                releaseLocks()
                _isServiceRunning.value = false
                _currentStreamingTitle.value = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_STREAM -> {
                val mediaTitle = intent.getStringExtra(EXTRA_MEDIA_TITLE) ?: "Canlı Medya Yayını"
                val tvName = intent.getStringExtra(EXTRA_TV_NAME) ?: "MediaDash TV"

                _isServiceRunning.value = true
                _currentStreamingTitle.value = mediaTitle

                val notification = buildNotification(mediaTitle, tvName)
                startForeground(NOTIFICATION_ID, notification)
            }
            else -> {
                val notification = buildNotification("Canlı Medya Aktarımı", "MediaDash TV")
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "MediaDash:LockScreenCastWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(12 * 60 * 60 * 1000L) // 12 saat maksimum koruma
                }
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null && wifiLock == null) {
                wifiLock = wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "MediaDash:LockScreenCastWifiLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (_: Exception) {}
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null

            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock = null
        } catch (_: Exception) {}
    }

    private fun buildNotification(mediaTitle: String, tvName: String): Notification {
        val openIntent = Intent(this, PhoneActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PhoneStreamingForegroundService::class.java).apply {
            action = ACTION_STOP_STREAM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📺 TV'ye Aktarılıyor: $mediaTitle")
            .setContentText("Hedef: $tvName • Ekran kilitliyken yayın devam ediyor")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Yayını Durdur", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MediaDash Canlı Yayın Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Telefon kilit ekranındayken TV'ye kesintisiz medya akışı bildirim kanalı"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        releaseLocks()
        _isServiceRunning.value = false
        _currentStreamingTitle.value = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
