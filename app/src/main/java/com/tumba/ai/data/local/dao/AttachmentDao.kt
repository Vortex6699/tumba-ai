package com.tumba.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tumba.ai.data.local.entity.AttachmentEntity

@Dao
interface AttachmentDao {

    @Insert
    suspend fun insertAll(entities: List<AttachmentEntity>)

    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    suspend fun forMessage(messageId: Long): List<AttachmentEntity>

    @Query(
        """
        SELECT path FROM attachments WHERE messageId IN
            (SELECT id FROM messages WHERE conversationId = :conversationId)
        """,
    )
    suspend fun pathsForConversation(conversationId: Long): List<String>

    @Query("SELECT path FROM attachments WHERE messageId = :messageId")
    suspend fun pathsForMessage(messageId: Long): List<String>

    @Query("SELECT path FROM attachments")
    suspend fun allPaths(): List<String>
}
