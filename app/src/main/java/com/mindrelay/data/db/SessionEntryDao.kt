package com.mindrelay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionEntryDao {
    @Query("SELECT * FROM session_entries WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun bySession(sessionId: Long): Flow<List<SessionEntryEntity>>

    @Query("SELECT * FROM session_entries ORDER BY createdAt ASC")
    fun all(): Flow<List<SessionEntryEntity>>

    @Insert
    suspend fun insert(item: SessionEntryEntity): Long

    @Update
    suspend fun update(item: SessionEntryEntity)

    @Query("DELETE FROM session_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM session_entries WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("SELECT * FROM session_entries")
    suspend fun listAll(): List<SessionEntryEntity>

    @Query("DELETE FROM session_entries")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(items: List<SessionEntryEntity>)
}
