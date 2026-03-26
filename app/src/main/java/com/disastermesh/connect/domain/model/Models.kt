package com.disastermesh.connect.domain.model

import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════
// MESSAGE PACKET — Transmitted over the mesh network
// Compact, serializable, TTL-managed
// ═══════════════════════════════════════════════════════════
@Serializable
data class MeshPacket(
    val messageId: String,          // UUID — deduplication key
    val senderId: String,           // Originator device ID
    val senderName: String,         // Human-readable name
    val recipientId: String?,       // null = broadcast
    val content: String,            // Message body
    val timestamp: Long,            // Epoch millis
    val ttl: Int,                   // Hop limit (default 8)
    val isBroadcast: Boolean,       // True = deliver to all
    val isEmergency: Boolean,       // True = priority routing
    val packetType: PacketType,     // Message type enum
    val locationPayload: LocationPayload? = null,  // Optional GPS
    val ackForMessageId: String? = null            // ACK reference
)

@Serializable
enum class PacketType {
    CHAT,           // Regular message
    BROADCAST,      // Emergency broadcast
    ACK,            // Delivery acknowledgment
    IDENTITY,       // Identity announcement
    LOCATION        // Location share
}

// ═══════════════════════════════════════════════════════════
// LOCATION PAYLOAD — Lightweight GPS packet
// ═══════════════════════════════════════════════════════════
@Serializable
data class LocationPayload(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val capturedAt: Long    // Epoch millis
)

// ═══════════════════════════════════════════════════════════
// IDENTITY PACKET — Broadcast on connect/discovery
// ═══════════════════════════════════════════════════════════
@Serializable
data class IdentityPacket(
    val deviceId: String,
    val displayName: String,
    val phoneHash: String?,     // SHA-256 of phone (optional)
    val publicKeyBase64: String // For E2E verification
)

// ═══════════════════════════════════════════════════════════
// DOMAIN MODELS — Internal app use
// ═══════════════════════════════════════════════════════════

enum class MessageStatus {
    PENDING,        // Stored, not yet sent
    SENT,           // Sent to at least one relay
    DELIVERED,      // ACK received from recipient
    FAILED          // TTL exhausted / no path
}

enum class ContactType {
    UNKNOWN,        // Discovered nearby, no contact info
    SAVED,          // In user's contact list
    FAMILY,         // Priority contact
    EMERGENCY       // Highest priority contact
}

enum class ConnectionStatus {
    DISCONNECTED,
    SEARCHING,
    CONNECTED
}

enum class BatteryMode {
    EMERGENCY,      // High scan frequency
    NORMAL,         // Standard
    SAVER           // Low scan frequency
}

data class PeerInfo(
    val endpointId: String,     // Nearby Connections endpoint
    val deviceId: String,       // App-level device ID
    val displayName: String,
    val connectedAt: Long,
    val rssi: Int = 0           // Signal strength estimate
)
