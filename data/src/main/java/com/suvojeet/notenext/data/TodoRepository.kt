package com.suvojeet.notenext.data

import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getAllTodos(): Flow<List<TodoItem>>
    fun getActiveTodos(): Flow<List<TodoItem>>
    fun getCompletedTodos(): Flow<List<TodoItem>>
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
    
    override fun getAllTodos(): Flow<List<TodoItem>> = todoDao.getAllTodos()
    
    override fun getActiveTodos(): Flow<List<TodoItem>> = todoDao.getActiveTodos()
    
    override fun getCompletedTodos(): Flow<List<TodoItem>> = todoDao.getCompletedTodos()
    
    override suspend fun getTodoById(id: Int): TodoItem? = todoDao.getTodoById(id)
    
    override suspend fun insertTodo(todo: TodoItem): Long = todoDao.insertTodo(todo)
    
    override suspend fun updateTodo(todo: TodoItem) = todoDao.updateTodo(todo)
    
    override suspend fun deleteTodo(todo: TodoItem) = todoDao.deleteTodo(todo)
    
    override suspend fun deleteAllCompleted() = todoDao.deleteAllCompleted()
    
    override fun getActiveCount(): Flow<Int> = todoDao.getActiveCount()
    
    override fun getCompletedCount(): Flow<Int> = todoDao.getCompletedCount()
}
