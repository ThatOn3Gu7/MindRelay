package com.mindrelay.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mindrelay.data.model.CaptureKind
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures WHERE archived = 0 ORDER BY createdAt DESC")
    fun active(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures ORDER BY createdAt DESC")
    fun all(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id")
    fun byId(id: Long): Flow<CaptureEntity?>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun one(id: Long): CaptureEntity?

    @Query("SELECT COUNT(*) FROM captures WHERE archived = 0")
    fun activeCount(): Flow<Int>

    @Query("SELECT * FROM captures WHERE kind = :kind")
    suspend fun byKind(kind: CaptureKind): List<CaptureEntity>

    @Insert
    suspend fun insert(item: CaptureEntity): Long

    @Update
    suspend fun update(item: CaptureEntity)

    @Delete
    suspend fun delete(item: CaptureEntity)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE captures SET projectId = NULL WHERE projectId = :projectId")
    suspend fun detachProject(projectId: Long)

    @Query("SELECT * FROM captures")
    suspend fun listAll(): List<CaptureEntity>

    @Query("DELETE FROM captures")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(items: List<CaptureEntity>)
}
