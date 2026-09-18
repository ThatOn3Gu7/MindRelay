package com.mindrelay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE archived = 0 ORDER BY revisitAt IS NULL, revisitAt ASC, createdAt DESC")
    fun active(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun all(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id")
    fun byId(id: Long): Flow<MemoryEntity?>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun one(id: Long): MemoryEntity?

    @Query("SELECT * FROM memories WHERE archived = 0 AND (revisitAt IS NULL OR revisitAt <= :now) ORDER BY revisitAt IS NULL, revisitAt ASC LIMIT :limit")
    fun dueForRevisit(now: Long, limit: Int): Flow<List<MemoryEntity>>

    /** Only memories actually due now ([revisitAt] set and not in the future). */
    @Query("SELECT * FROM memories WHERE archived = 0 AND revisitAt IS NOT NULL AND revisitAt <= :now ORDER BY revisitAt ASC LIMIT :limit")
    fun dueNow(now: Long, limit: Int): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE projectId = :projectId ORDER BY revisitAt IS NULL, revisitAt ASC, createdAt DESC")
    fun byProject(projectId: Long): Flow<List<MemoryEntity>>

    @Insert
    suspend fun insert(item: MemoryEntity): Long

    @Update
    suspend fun update(item: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE memories SET projectId = NULL WHERE projectId = :projectId")
    suspend fun detachProject(projectId: Long)

    @Query("SELECT * FROM memories")
    suspend fun listAll(): List<MemoryEntity>

    @Query("DELETE FROM memories")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(items: List<MemoryEntity>)
}
