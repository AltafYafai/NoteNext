package com.suvojeet.notenext.data

import androidx.room.*
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, priority DESC, dueDate ASC, createdAt DESC")
    fun getAllTodos(): PagingSource<Int, TodoItem>
    
    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC, createdAt DESC")
    fun getActiveTodos(): PagingSource<Int, TodoItem>
    
    @Query("SELECT * FROM todos WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTodos(): PagingSource<Int, TodoItem>
    
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Int): TodoItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long
    
    @Update
    suspend fun updateTodo(todo: TodoItem)
    
    @Delete
    suspend fun deleteTodo(todo: TodoItem)
    
    @Query("DELETE FROM todos WHERE isCompleted = 1")
    suspend fun deleteAllCompleted()
    
    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0")
    fun getActiveCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>
}
