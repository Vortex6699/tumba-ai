package com.tumba.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tumba.ai.data.local.dao.AttachmentDao
import com.tumba.ai.data.local.dao.ConversationDao
import com.tumba.ai.data.local.dao.CustomProviderDao
import com.tumba.ai.data.local.dao.LocalModelDao
import com.tumba.ai.data.local.dao.MessageDao
import com.tumba.ai.data.local.entity.AttachmentEntity
import com.tumba.ai.data.local.entity.ConversationEntity
import com.tumba.ai.data.local.entity.CustomProviderEntity
import com.tumba.ai.data.local.entity.LocalModelEntity
import com.tumba.ai.data.local.entity.MessageEntity
import com.tumba.ai.data.local.Converters

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AttachmentEntity::class,
        CustomProviderEntity::class,
        LocalModelEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TumbaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun customProviderDao(): CustomProviderDao
    abstract fun localModelDao(): LocalModelDao

    companion object {
        const val NAME = "tumba.db"
    }
}
