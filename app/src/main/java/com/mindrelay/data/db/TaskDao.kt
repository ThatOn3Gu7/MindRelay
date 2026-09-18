package com.mindrelay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, createdAt DESC")
    fun open(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY done ASC, createdAt DESC")
    fun all(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId")
    fun byProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE captureId = :captureId")
    fun byCapture(captureId: Long): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(item: TaskEntity): Long

    @Update
    suspend fun update(item: TaskEntity)

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun clearByProject(projectId: Long)

    @Query("SELECT * FROM tasks")
    suspend fun listAll(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(items: List<TaskEntity>)
}
