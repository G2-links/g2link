package com.disastermesh.connect.di

import android.content.Context
import androidx.room.Room
import com.disastermesh.connect.data.local.MeshDatabase
import com.disastermesh.connect.data.local.dao.*
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
            "disastermesh.db"
        )
            .fallbackToDestructiveMigration() // Upgrade in production with migrations
            .build()
    }

    @Provides fun provideMessageDao(db: MeshDatabase): MessageDao = db.messageDao()

    @Provides fun provideContactDao(db: MeshDatabase): ContactDao = db.contactDao()

    @Provides fun providePeerDao(db: MeshDatabase): PeerDao = db.peerDao()

    @Provides fun provideDeliveredMessageCacheDao(db: MeshDatabase): DeliveredMessageCacheDao =
        db.deliveredMessageCacheDao()
}
