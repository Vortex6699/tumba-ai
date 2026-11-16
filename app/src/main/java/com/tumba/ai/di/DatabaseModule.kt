package com.tumba.ai.di

import android.content.Context
import androidx.room.Room
import com.tumba.ai.data.local.DatabaseMigrations
import com.tumba.ai.data.local.TumbaDatabase
import com.tumba.ai.data.local.dao.AttachmentDao
import com.tumba.ai.data.local.dao.ConversationDao
import com.tumba.ai.data.local.dao.CustomProviderDao
import com.tumba.ai.data.local.dao.LocalModelDao
import com.tumba.ai.data.local.dao.MessageDao
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
    fun provideDatabase(@ApplicationContext context: Context): TumbaDatabase =
        Room.databaseBuilder(context, TumbaDatabase::class.java, TumbaDatabase.NAME)
            .addMigrations(*DatabaseMigrations.ALL)
            .build()

    @Provides
    fun provideConversationDao(database: TumbaDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMessageDao(database: TumbaDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideAttachmentDao(database: TumbaDatabase): AttachmentDao = database.attachmentDao()

    @Provides
    fun provideCustomProviderDao(database: TumbaDatabase): CustomProviderDao =
        database.customProviderDao()

    @Provides
    fun provideLocalModelDao(database: TumbaDatabase): LocalModelDao = database.localModelDao()
}
