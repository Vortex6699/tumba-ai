package com.tumba.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.tumba.ai.data.local.entity.MessageEntity
import com.tumba.ai.data.local.model.MessageWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Transaction
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeForConversation(conversationId: Long): Flow<List<MessageWithAttachments>>

    @Transaction
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    suspend fun getForConversation(conversationId: Long): List<MessageWithAttachments>

    @Insert
    suspend fun insert(entity: MessageEntity): Long

    @Query("UPDATE messages SET content = :content, status = :status, error = :error WHERE id = :id")
    suspend fun update(id: Long, content: String, status: String, error: String?)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?
}
