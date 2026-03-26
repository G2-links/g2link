package com.disastermesh.connect.data.local.dao

import androidx.room.*
import com.disastermesh.connect.data.local.entity.*
import com.disastermesh.connect.domain.model.ContactType
import com.disastermesh.connect.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════════════════════════
// MESSAGE DAO
// ═══════════════════════════════════════════════════════════
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    /** All messages in a conversation with a contact (both directions) */
    @Query("""
        SELECT * FROM messages
        WHERE (senderId = :myDeviceId AND recipientId = :contactDeviceId)
           OR (senderId = :contactDeviceId AND recipientId = :myDeviceId)
        ORDER BY timestamp ASC
    """)
    fun getConversationFlow(myDeviceId: String, contactDeviceId: String): Flow<List<MessageEntity>>

    /** Broadcast messages visible to everyone */
    @Query("""
        SELECT * FROM messages
        WHERE isBroadcast = 1
        ORDER BY timestamp DESC
        LIMIT 200
    """)
    fun getBroadcastMessagesFlow(): Flow<List<MessageEntity>>

    /** Latest message per contact for chat list preview */
    @Query("""
        SELECT * FROM messages
        WHERE id IN (
            SELECT MAX(id) FROM messages
            WHERE isBroadcast = 0
            GROUP BY CASE
                WHEN senderId = :myDeviceId THEN recipientId
                ELSE senderId
            END
        )
        ORDER BY timestamp DESC
    """)
    fun getLatestMessagesPerContact(myDeviceId: String): Flow<List<MessageEntity>>

    /** Pending messages to retry delivery */
    @Query("""
        SELECT * FROM messages
        WHERE status = 'PENDING' 
          AND retryCount < 10
          AND isIncoming = 0
        ORDER BY isEmergency DESC, timestamp ASC
    """)
    suspend fun getPendingOutboundMessages(): List<MessageEntity>

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET retryCount = retryCount + 1, lastRetryAt = :now WHERE messageId = :messageId")
    suspend fun incrementRetryCount(messageId: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM messages WHERE timestamp < :cutoff AND status = 'DELIVERED'")
    suspend fun pruneOldDeliveredMessages(cutoff: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE recipientId = :myDeviceId AND status != 'DELIVERED' AND isIncoming = 1")
    fun getUnreadCountFlow(myDeviceId: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE (senderId = :contactId OR recipientId = :contactId)
          AND isIncoming = 1
          AND status != 'DELIVERED'
    """)
    fun getUnreadCountForContactFlow(contactId: String): Flow<Int>
}

// ═══════════════════════════════════════════════════════════
// CONTACT DAO
// ═══════════════════════════════════════════════════════════
@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContact(contact: ContactEntity): Long

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getContactByDeviceId(deviceId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getContactByPhone(phone: String): ContactEntity?

    @Query("SELECT * FROM contacts ORDER BY contactType DESC, displayName ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("""
        SELECT * FROM contacts
        WHERE contactType = 'FAMILY' OR contactType = 'EMERGENCY'
        ORDER BY contactType DESC
    """)
    fun getPriorityContactsFlow(): Flow<List<ContactEntity>>

    @Query("UPDATE contacts SET lastSeenAt = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLastSeen(deviceId: String, timestamp: Long)

    @Query("UPDATE contacts SET contactType = :type WHERE deviceId = :deviceId")
    suspend fun updateContactType(deviceId: String, type: ContactType)

    @Query("SELECT * FROM contacts WHERE isBlocked = 0 ORDER BY displayName ASC")
    suspend fun getAllActiveContacts(): List<ContactEntity>
}

// ═══════════════════════════════════════════════════════════
// PEER DAO
// ═══════════════════════════════════════════════════════════
@Dao
interface PeerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePeer(peer: PeerEntity): Long

    @Query("SELECT * FROM peers WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getPeerByDeviceId(deviceId: String): PeerEntity?

    @Query("SELECT * FROM peers WHERE isCurrentlyConnected = 1")
    fun getConnectedPeersFlow(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE isCurrentlyConnected = 1")
    suspend fun getConnectedPeers(): List<PeerEntity>

    @Query("UPDATE peers SET isCurrentlyConnected = 0")
    suspend fun clearAllConnections()

    @Query("""
        UPDATE peers SET 
            isCurrentlyConnected = :connected,
            lastConnectedAt = :timestamp
        WHERE deviceId = :deviceId
    """)
    suspend fun updateConnectionState(deviceId: String, connected: Boolean, timestamp: Long)

    @Query("SELECT COUNT(*) FROM peers WHERE isCurrentlyConnected = 1")
    fun getConnectedPeerCountFlow(): Flow<Int>
}

// ═══════════════════════════════════════════════════════════
// DELIVERED MESSAGE CACHE DAO
// ═══════════════════════════════════════════════════════════
@Dao
interface DeliveredMessageCacheDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSeen(entry: DeliveredMessageCacheEntity): Long

    @Query("SELECT COUNT(*) FROM delivered_message_cache WHERE messageId = :messageId")
    suspend fun hasSeenMessage(messageId: String): Int

    /** Keep only the most recent 1000 entries */
    @Query("""
        DELETE FROM delivered_message_cache
        WHERE id NOT IN (
            SELECT id FROM delivered_message_cache
            ORDER BY seenAt DESC
            LIMIT 1000
        )
    """)
    suspend fun pruneExcess()

    @Query("DELETE FROM delivered_message_cache WHERE seenAt < :cutoff")
    suspend fun pruneOld(cutoff: Long)
}
