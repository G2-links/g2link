package com.g2link.connect.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.g2link.connect.domain.model.ContactType
import com.g2link.connect.domain.model.MessageStatus

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["messageId"], unique = true),
        Index(value = ["senderId"]),
        Index(value = ["recipientId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val recipientId: String?,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus,
    val isIncoming: Boolean,
    val isBroadcast: Boolean,
    val isEmergency: Boolean,
    val ttl: Int,
    val retryCount: Int = 0,
    val lastRetryAt: Long = 0L,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationAccuracy: Float? = null
)

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["deviceId"], unique = true)]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val displayName: String,
    val phoneNumber: String? = null,
    val publicKeyBase64: String? = null,
    val contactType: ContactType = ContactType.UNKNOWN,
    val lastSeenAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val avatarColor: Int = 0
)

@Entity(
    tableName = "peers",
    indices = [Index(value = ["deviceId"], unique = true)]
)
data class PeerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val displayName: String,
    val endpointId: String,
    val lastConnectedAt: Long,
    val connectionCount: Int = 1,
    val isCurrentlyConnected: Boolean = false
)

@Entity(
    tableName = "delivered_message_cache",
    indices = [Index(value = ["messageId"], unique = true)]
)
data class DeliveredMessageCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val seenAt: Long = System.currentTimeMillis()
)
