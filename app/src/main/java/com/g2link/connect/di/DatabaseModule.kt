package com.g2link.connect.di

import android.content.Context
import androidx.room.Room
import com.g2link.connect.data.local.MeshDatabase
import com.g2link.connect.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMeshDatabase(@ApplicationContext context: Context): MeshDatabase {
        return Room.databaseBuilder(
            context,
            MeshDatabase::class.java,
            "g2link.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides fun provideMessageDao(db: MeshDatabase): MessageDao = db.messageDao()
    @Provides fun provideContactDao(db: MeshDatabase): ContactDao = db.contactDao()
    @Provides fun providePeerDao(db: MeshDatabase): PeerDao = db.peerDao()
    @Provides fun provideDeliveredMessageCacheDao(db: MeshDatabase): DeliveredMessageCacheDao =
        db.deliveredMessageCacheDao()
}
