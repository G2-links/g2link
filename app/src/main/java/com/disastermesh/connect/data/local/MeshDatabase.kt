package com.disastermesh.connect.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.disastermesh.connect.data.local.dao.*
import com.disastermesh.connect.data.local.entity.*
import com.disastermesh.connect.domain.model.ContactType
import com.disastermesh.connect.domain.model.MessageStatus

// ═══════════════════════════════════════════════════════════
// TYPE CONVERTERS
// ═══════════════════════════════════════════════════════════
class Converters {

    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)

    @TypeConverter
    fun fromContactType(type: ContactType): String = type.name

    @TypeConverter
    fun toContactType(value: String): ContactType = ContactType.valueOf(value)
}

// ═══════════════════════════════════════════════════════════
// ROOM DATABASE
// ═══════════════════════════════════════════════════════════
@Database(
    entities = [
        MessageEntity::class,
        ContactEntity::class,
        PeerEntity::class,
        DeliveredMessageCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MeshDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun peerDao(): PeerDao
    abstract fun deliveredMessageCacheDao(): DeliveredMessageCacheDao
}
