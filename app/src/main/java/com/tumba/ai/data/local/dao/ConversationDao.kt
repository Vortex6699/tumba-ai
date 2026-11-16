package com.tumba.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tumba.ai.data.local.entity.ConversationEntity
import com.tumba.ai.data.local.model.ConversationSummaryRow
import com.tumba.ai.data.local.model.StatsRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query(
        """
        SELECT c.id, c.title, c.createdAt, c.updatedAt, c.providerId, c.modelId, c.pinned,
            (SELECT COUNT(*) FROM messages m WHERE m.conversationId = c.id) AS messageCount,
            (SELECT m.content FROM messages m WHERE m.conversationId = c.id
                ORDER BY m.timestamp DESC, m.id DESC LIMIT 1) AS preview
        FROM conversations c
        ORDER BY c.pinned DESC, c.updatedAt DESC
        """,
    )
    fun observeAll(): Flow<List<ConversationSummaryRow>>

    @Query(
        """
        SELECT c.id, c.title, c.createdAt, c.updatedAt, c.providerId, c.modelId, c.pinned,
            (SELECT COUNT(*) FROM messages m WHERE m.conversationId = c.id) AS messageCount,
            (SELECT m.content FROM messages m WHERE m.conversationId = c.id
                ORDER BY m.timestamp DESC, m.id DESC LIMIT 1) AS preview
        FROM conversations c
        WHERE c.title LIKE '%' || :query || '%'
            OR EXISTS (
                SELECT 1 FROM messages m2
                WHERE m2.conversationId = c.id AND m2.content LIKE '%' || :query || '%'
            )
        ORDER BY c.pinned DESC, c.updatedAt DESC
        """,
    )
    fun search(query: String): Flow<List<ConversationSummaryRow>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAllOnce(): List<ConversationEntity>

    @Insert
    suspend fun insert(entity: ConversationEntity): Long

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, title: String, updatedAt: Long)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    @Query("UPDATE conversations SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()

    @Query(
        """
        SELECT (SELECT COUNT(*) FROM conversations) AS conversations,
               (SELECT COUNT(*) FROM messages) AS messages
        """,
    )
    fun observeStats(): Flow<StatsRow>
}
