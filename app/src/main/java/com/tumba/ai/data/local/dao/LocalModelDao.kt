package com.tumba.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tumba.ai.data.local.entity.LocalModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalModelDao {

    @Query("SELECT * FROM local_models ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_models ORDER BY importedAt DESC")
    suspend fun getAll(): List<LocalModelEntity>

    @Query("SELECT * FROM local_models WHERE id = :id")
    suspend fun getById(id: String): LocalModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalModelEntity)

    @Query("DELETE FROM local_models WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM local_models")
    suspend fun totalSize(): Long
}
