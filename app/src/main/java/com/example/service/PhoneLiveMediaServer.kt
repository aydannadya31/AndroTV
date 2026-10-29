package com.example.service

import android.content.Context
import android.net.Uri
import com.example.data.local.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

/**
 * TV'ye dosya yüklemeye ve TV hafızasını doldurmaya gerek kalmadan,
 * Telefondaki medya verilerini (Video, Müzik, Ses, Fotoğraf) doğrudan telefon üzerinden
 * canlı HTTP/Byte stream olarak TV'ye aktaran yerel mikro sunucu.
 */
class PhoneLiveMediaServer(private val context: Context) {

    companion object {
        const val STREAM_PORT = 8096
    }

    private var serverSocket: ServerSocket? = null
    private var activeMediaUri: Uri? = null
    private var activeMediaType: MediaType = MediaType.MOVIE
    private var activeMimeType: String = "video/mp4"
    private var activeTitle: String = "Canlı Yayın"

    fun setLiveMedia(title: String, type: MediaType, uri: Uri?, mimeType: String? = null) {
        activeTitle = title
        activeMediaType = type
        activeMediaUri = uri
        activeMimeType = mimeType ?: when (type) {
            MediaType.PHOTO -> "image/jpeg"
            MediaType.MUSIC -> "audio/mpeg"
            MediaType.MOVIE, MediaType.SERIES -> "video/mp4"
            else -> "video/mp4"
        }
    }

    fun startServer(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket?.close()
                val server = ServerSocket(STREAM_PORT)
                serverSocket = server

                while (isActive && !server.isClosed) {
                    try {
                        val client = server.accept()
                        scope.launch(Dispatchers.IO) {
                            handleStreamingRequest(client)
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun handleStreamingRequest(client: Socket) = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val output = BufferedOutputStream(client.getOutputStream())

            val requestLine = reader.readLine() ?: return@withContext
            var headerLine: String? = reader.readLine()
            var rangeHeader: String? = null

            while (!headerLine.isNullOrEmpty()) {
                if (headerLine.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = headerLine
                }
                headerLine = reader.readLine()
            }

            val uri = activeMediaUri
            if (uri == null) {
                // Sentetik test verisi veya doğrudan RAM belleğinden akış
                val sampleContent = "MediaDash Hub Live Streaming Audio/Video Sample Frame".toByteArray()
                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: ${sampleContent.size}\r\n" +
                        "Access-Control-Allow-Origin: *\r\n\r\n"
                output.write(response.toByteArray())
                output.write(sampleContent)
                output.flush()
                return@withContext
            }

            val contentResolver = context.contentResolver
            val fileLength = try {
                contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            } catch (_: Exception) {
                -1L
            }

            inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                val notFound = "HTTP/1.1 404 Not Found\r\n\r\n"
                output.write(notFound.toByteArray())
                output.flush()
                return@withContext
            }

            if (fileLength > 0) {
                val headers = StringBuilder().apply {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: $activeMimeType\r\n")
                    append("Content-Length: $fileLength\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    append("Access-Control-Allow-Origin: *\r\n")
                    append("Connection: keep-alive\r\n\r\n")
                }.toString()
                output.write(headers.toByteArray())

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            } else {
                val headers = StringBuilder().apply {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: $activeMimeType\r\n")
                    append("Access-Control-Allow-Origin: *\r\n\r\n")
                }.toString()
                output.write(headers.toByteArray())

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
        } catch (_: Exception) {
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        activeMediaUri = null
    }
}
