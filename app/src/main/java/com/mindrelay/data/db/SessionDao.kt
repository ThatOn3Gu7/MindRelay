package com.mindrelay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY startedAt DESC")
    fun byProject(projectId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun all(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun byId(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun one(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE projectId = :projectId AND status = 'Active' ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeForProject(projectId: Long): SessionEntity?

    @Query("SELECT COUNT(*) FROM sessions WHERE projectId = :projectId")
    fun sessionCount(projectId: Long): Flow<Int>

    @Insert
    suspend fun insert(item: SessionEntity): Long

    @Update
    suspend fun update(item: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(items: List<SessionEntity>)

    @Query("SELECT * FROM sessions")
    suspend fun listAll(): List<SessionEntity>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
