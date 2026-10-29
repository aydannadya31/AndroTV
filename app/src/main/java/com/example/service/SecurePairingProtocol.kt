package com.example.service

import com.example.data.local.MediaType
import org.json.JSONObject
import java.security.SecureRandom

enum class PairingState {
    UNPAIRED,
    PAIRING_IN_PROGRESS,
    PENDING_TV_APPROVAL,
    PAIRED,
    REJECTED
}

enum class PacketType {
    PAIR_REQUEST,
    PAIR_RESPONSE,
    MEDIA_START,
    CHUNK_DATA,
    MEDIA_COMPLETE,
    LIVE_STREAM_START,
    PING,
    ERROR
}

data class PairingRequest(
    val deviceId: String,
    val deviceName: String,
    val enteredPin: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PairingSession(
    val deviceId: String,
    val deviceName: String,
    val sessionToken: String,
    val pairedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L // 24 saat geçerli
)

data class MediaTransferMetadata(
    val transferId: String,
    val sessionToken: String,
    val fileName: String,
    val fileSize: Long,
    val mediaType: MediaType,
    val mimeType: String,
    val totalChunks: Int
)

data class TransferProgress(
    val transferId: String,
    val fileName: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val progressPercent: Int,
    val transferSpeedKbps: Double,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

class SecurePairingProtocol {

    companion object {
        const val MAGIC_HEADER = "MDHP_V1"
        const val SOCKET_PORT = 8095
        const val CHUNK_SIZE = 64 * 1024 // 64 KB paket boyutu
    }

    private val secureRandom = SecureRandom()

    fun generatePinCode(): String {
        val pin = 100000 + secureRandom.nextInt(900000)
        return pin.toString()
    }

    fun generateSessionToken(deviceId: String): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        val tokenHex = bytes.joinToString("") { "%02x".format(it) }
        return "mdhp_${deviceId.take(6)}_$tokenHex"
    }

    fun buildPacket(type: PacketType, payload: JSONObject): String {
        val root = JSONObject()
        root.put("magic", MAGIC_HEADER)
        root.put("type", type.name)
        root.put("timestamp", System.currentTimeMillis())
        root.put("payload", payload)
        return root.toString() + "\n"
    }

    fun parsePacket(rawText: String): Pair<PacketType, JSONObject>? {
        return try {
            val root = JSONObject(rawText.trim())
            val magic = root.optString("magic")
            if (magic != MAGIC_HEADER) return null
            val typeStr = root.optString("type")
            val type = PacketType.valueOf(typeStr)
            val payload = root.optJSONObject("payload") ?: JSONObject()
            Pair(type, payload)
        } catch (_: Exception) {
            null
        }
    }
}
