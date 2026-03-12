package com.suvojeet.notenext.data

import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig

interface TodoRepository {
    fun getPagedTodos(): Flow<PagingData<TodoItem>>
    fun getPagedActiveTodos(): Flow<PagingData<TodoItem>>
    fun getPagedCompletedTodos(): Flow<PagingData<TodoItem>>
    suspend fun getTodoById(id: Int): TodoItem?
    suspend fun insertTodo(todo: TodoItem): Long
    suspend fun updateTodo(todo: TodoItem)
    suspend fun deleteTodo(todo: TodoItem)
    suspend fun deleteAllCompleted()
    fun getActiveCount(): Flow<Int>
    fun getCompletedCount(): Flow<Int>
}

class TodoRepositoryImpl(
    private val todoDao: TodoDao
) : TodoRepository {
    
    override fun getPagedTodos(): Flow<PagingData<TodoItem>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { todoDao.getAllTodos() }
        ).flow
    }

    override fun getPagedActiveTodos(): Flow<PagingData<TodoItem>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { todoDao.getActiveTodos() }
        ).flow
    }

    override fun getPagedCompletedTodos(): Flow<PagingData<TodoItem>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { todoDao.getCompletedTodos() }
        ).flow
    }
    
    override suspend fun getTodoById(id: Int): TodoItem? = todoDao.getTodoById(id)
    
    override suspend fun insertTodo(todo: TodoItem): Long = todoDao.insertTodo(todo)
    
    override suspend fun updateTodo(todo: TodoItem) = todoDao.updateTodo(todo)
    
    override suspend fun deleteTodo(todo: TodoItem) = todoDao.deleteTodo(todo)
    
    override suspend fun deleteAllCompleted() = todoDao.deleteAllCompleted()
    
    override fun getActiveCount(): Flow<Int> = todoDao.getActiveCount()
    
    override fun getCompletedCount(): Flow<Int> = todoDao.getCompletedCount()
}
