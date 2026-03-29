package com.g2link.connect.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MeshPacket(
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val recipientId: String?,
    val content: String,
    val timestamp: Long,
    val ttl: Int,
    val isBroadcast: Boolean,
    val isEmergency: Boolean,
    val packetType: PacketType,
    val locationPayload: LocationPayload? = null,
    val ackForMessageId: String? = null
)

@Serializable
enum class PacketType {
    CHAT, BROADCAST, ACK, IDENTITY, LOCATION
}

@Serializable
data class LocationPayload(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val capturedAt: Long
)

@Serializable
data class IdentityPacket(
    val deviceId: String,
    val displayName: String,
    val phoneHash: String?,
    val publicKeyBase64: String
)

enum class MessageStatus { PENDING, SENT, DELIVERED, FAILED }

enum class ContactType { UNKNOWN, SAVED, FAMILY, EMERGENCY }

enum class ConnectionStatus { DISCONNECTED, SEARCHING, CONNECTED }

enum class BatteryMode { EMERGENCY, NORMAL, SAVER }

data class PeerInfo(
    val endpointId: String,
    val deviceId: String,
    val displayName: String,
    val connectedAt: Long,
    val rssi: Int = 0
)
