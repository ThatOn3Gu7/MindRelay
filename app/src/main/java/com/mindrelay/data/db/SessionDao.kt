package com.mindrelay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mindrelay.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY startedAt DESC")
    fun byProject(projectId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY startedAt DESC")
    suspend fun byProjectOneShot(projectId: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun all(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun byId(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun one(id: Long): SessionEntity?

    /** Open (active) sessions, for transactional mediation of the uniqueness invariant. */
    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE'")
    suspend fun openSessions(): List<SessionEntity>

    /** Highest display number used so far on a project (0 when none exist). */
    @Query("SELECT COALESCE(MAX(displayNumber), 0) FROM sessions WHERE projectId = :projectId")
    suspend fun maxDisplayNumber(projectId: Long): Int

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
