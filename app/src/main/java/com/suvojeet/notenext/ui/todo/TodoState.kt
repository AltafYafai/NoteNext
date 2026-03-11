package com.suvojeet.notenext.ui.todo

import com.suvojeet.notenext.data.TodoItem

data class TodoState(
    val todos: List<TodoItem> = emptyList(),
    val filter: TodoFilter = TodoFilter.All,
    val isLoading: Boolean = true,
    val showAddEditDialog: Boolean = false,
    val editingTodo: TodoItem? = null,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    
    // AI Todo
    val isGenerating: Boolean = false,
    val showAiTodoDialog: Boolean = false,
    val aiTodoResult: List<Pair<String, String>> = emptyList()
)
