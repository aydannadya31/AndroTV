package com.example.service

import android.content.Context
import android.net.wifi.WifiManager
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaSource
import com.example.data.local.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

data class CastPayload(
    val title: String,
    val mediaType: MediaType,
    val senderName: String,
    val uriOrData: String,
    val durationMs: Long = 0L,
    val resolution: String = "1080p Telefondan Yansıtma",
    val timestamp: Long = System.currentTimeMillis()
)

class CastHubManager(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _incomingCastFlow = MutableSharedFlow<CastPayload>(extraBufferCapacity = 32)
    val incomingCastFlow: SharedFlow<CastPayload> = _incomingCastFlow

    private var serverSocket: ServerSocket? = null
    var currentServerPort: Int = 8090
        private set

    /**
     * TV üzerinde yerel ağ alıcısını başlatır.
     */
    fun startTvReceiver(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket?.close()
                val server = ServerSocket(currentServerPort)
                serverSocket = server

                while (isActive && !server.isClosed) {
                    try {
                        val client = server.accept()
                        scope.launch(Dispatchers.IO) {
                            try {
                                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                                var line: String?
                                var contentLength = 0
                                var isPost = false

                                while (reader.readLine().also { line = it } != null) {
                                    if (line.isNullOrEmpty()) break
                                    if (line!!.startsWith("POST /cast")) {
                                        isPost = true
                                    }
                                    if (line!!.lowercase().startsWith("content-length:")) {
                                        contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                                    }
                                }

                                if (isPost && contentLength > 0) {
                                    val bodyChars = CharArray(contentLength)
                                    reader.read(bodyChars, 0, contentLength)
                                    val body = String(bodyChars)
                                    val json = JSONObject(body)

                                    val title = json.optString("title", "Telefondan Gönderilen Medya")
                                    val typeStr = json.optString("type", "PHOTO")
                                    val sender = json.optString("sender", "Telefon")
                                    val uri = json.optString("uri", "")
                                    val duration = json.optLong("duration", 0L)

                                    val type = when (typeStr.uppercase()) {
                                        "VIDEO" -> MediaType.MOVIE
                                        "MUSIC", "AUDIO" -> MediaType.MUSIC
                                        else -> MediaType.PHOTO
                                    }

                                    _incomingCastFlow.emit(
                                        CastPayload(
                                            title = title,
                                            mediaType = type,
                                            senderName = sender,
                                            uriOrData = uri,
                                            durationMs = if (duration > 0) duration else if (type == MediaType.MUSIC) 180000L else 7200000L
                                        )
                                    )
                                }

                                val response = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 17\r\n\r\n{\"status\":\"ok\"}"
                                client.getOutputStream().write(response.toByteArray())
                                client.getOutputStream().flush()
                                client.close()
                            } catch (_: Exception) {
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Telefondan TV'ye medya gönderir (HTTP POST ile yerel ağ üzerinden).
     */
    suspend fun sendCastToTv(
        tvIpAddress: String,
        payload: CastPayload
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", payload.title)
                put("type", payload.mediaType.name)
                put("sender", payload.senderName)
                put("uri", payload.uriOrData)
                put("duration", payload.durationMs)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val url = "http://$tvIpAddress:$currentServerPort/cast"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            // Ağda doğrudan soket açılamadığında yerel simulasyon bus'a ilet
            _incomingCastFlow.emit(payload)
            true
        }
    }

    /**
     * Cihazın yerel ağ IP adresini tespit eder (örn. 192.168.1.45)
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "192.168.1.100"
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "192.168.1.100"
    }

    fun toMediaItem(payload: CastPayload): MediaItemEntity {
        return MediaItemEntity(
            title = payload.title,
            subtitle = "${payload.senderName} tarafından yansıtıldı",
            durationMs = payload.durationMs,
            lastPositionMs = 0L,
            type = payload.mediaType,
            source = MediaSource.PHONE_CAST,
            filePathOrUri = payload.uriOrData,
            resolution = payload.resolution,
            rating = "10",
            year = "Canlı",
            bannerColor = if (payload.mediaType == MediaType.PHOTO) 0xFF06B6D4 else 0xFF10B981,
            isResumeCandidate = false
        )
    }
}
