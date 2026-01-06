package com.suvojeet.notenext.ui.todo

import com.suvojeet.notenext.data.TodoItem

sealed class TodoEvent {
    data class AddTodo(val title: String, val description: String, val priority: Int, val dueDate: Long?) : TodoEvent()
    data class UpdateTodo(val todo: TodoItem) : TodoEvent()
    data class DeleteTodo(val todo: TodoItem) : TodoEvent()
    data class ToggleComplete(val todo: TodoItem) : TodoEvent()
    data class SetFilter(val filter: TodoFilter) : TodoEvent()
    object DeleteAllCompleted : TodoEvent()
    object ShowAddDialog : TodoEvent()
    data class ShowEditDialog(val todo: TodoItem) : TodoEvent()
    object DismissDialog : TodoEvent()
    data class SaveTodo(val title: String, val description: String, val priority: Int, val dueDate: Long?) : TodoEvent()
}
