package com.tumba.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tumba.ai.data.local.entity.CustomProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomProviderDao {

    @Query("SELECT * FROM custom_providers ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CustomProviderEntity>>

    @Query("SELECT * FROM custom_providers ORDER BY createdAt ASC")
    suspend fun getAll(): List<CustomProviderEntity>

    @Query("SELECT * FROM custom_providers WHERE id = :id")
    suspend fun getById(id: String): CustomProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomProviderEntity)

    @Query("DELETE FROM custom_providers WHERE id = :id")
    suspend fun delete(id: String)
}
