package com.suvojeet.notenext.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.data.TodoRepository
import com.suvojeet.notenext.data.repository.GroqRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TodoUiEvent {
    data class ShowToast(val message: String) : TodoUiEvent()
}

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository,
    private val groqRepository: GroqRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TodoState())
    val state: StateFlow<TodoState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TodoUiEvent>()
    val events: SharedFlow<TodoUiEvent> = _events.asSharedFlow()

    init {
        observeTodos()
    }

    private fun observeTodos() {
        combine(
            repository.getAllTodos(),
            repository.getActiveCount(),
            repository.getCompletedCount()
        ) { todos, activeCount, completedCount ->
            val filteredTodos = when (_state.value.filter) {
                is TodoFilter.All -> todos
                is TodoFilter.Active -> todos.filter { !it.isCompleted }
                is TodoFilter.Completed -> todos.filter { it.isCompleted }
            }
            _state.value = _state.value.copy(
                todos = filteredTodos,
                isLoading = false,
                activeCount = activeCount,
                completedCount = completedCount
            )
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: TodoEvent) {
        when (event) {
            is TodoEvent.AddTodo -> {
                viewModelScope.launch {
                    val todo = TodoItem(
                        title = event.title,
                        description = event.description,
                        priority = event.priority,
                        dueDate = event.dueDate,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertTodo(todo)
                }
            }
            is TodoEvent.UpdateTodo -> {
                viewModelScope.launch {
                    repository.updateTodo(event.todo)
                }
            }
            is TodoEvent.DeleteTodo -> {
                viewModelScope.launch {
                    repository.deleteTodo(event.todo)
                }
            }
            is TodoEvent.ToggleComplete -> {
                viewModelScope.launch {
                    val updatedTodo = event.todo.copy(
                        isCompleted = !event.todo.isCompleted,
                        completedAt = if (!event.todo.isCompleted) System.currentTimeMillis() else null
                    )
                    repository.updateTodo(updatedTodo)
                }
            }
            is TodoEvent.SetFilter -> {
                _state.value = _state.value.copy(filter = event.filter)
                observeTodos() // Re-observe with new filter
            }
            is TodoEvent.DeleteAllCompleted -> {
                viewModelScope.launch {
                    repository.deleteAllCompleted()
                }
            }
            is TodoEvent.ShowAddDialog -> {
                _state.value = _state.value.copy(
                    showAddEditDialog = true,
                    editingTodo = null
                )
            }
            is TodoEvent.ShowEditDialog -> {
                _state.value = _state.value.copy(
                    showAddEditDialog = true,
                    editingTodo = event.todo
                )
            }
            is TodoEvent.DismissDialog -> {
                _state.value = _state.value.copy(
                    showAddEditDialog = false,
                    editingTodo = null
                )
            }
            is TodoEvent.SaveTodo -> {
                viewModelScope.launch {
                    val editingTodo = _state.value.editingTodo
                    if (editingTodo != null) {
                        // Update existing todo
                        val updatedTodo = editingTodo.copy(
                            title = event.title,
                            description = event.description,
                            priority = event.priority,
                            dueDate = event.dueDate
                        )
                        repository.updateTodo(updatedTodo)
                    } else {
                        // Create new todo
                        val todo = TodoItem(
                            title = event.title,
                            description = event.description,
                            priority = event.priority,
                            dueDate = event.dueDate,
                            createdAt = System.currentTimeMillis()
                        )
                        repository.insertTodo(todo)
                    }
                    _state.value = _state.value.copy(
                        showAddEditDialog = false,
                        editingTodo = null
                    )
                }
            }
            is TodoEvent.ShowAiTodoDialog -> {
                _state.value = _state.value.copy(showAiTodoDialog = true)
            }
            is TodoEvent.DismissAiTodoDialog -> {
                _state.value = _state.value.copy(showAiTodoDialog = false)
            }
            is TodoEvent.GenerateAiTodos -> {
                viewModelScope.launch {
                    groqRepository.generateTodos(event.input)
                        .onStart { _state.value = _state.value.copy(isGenerating = true) }
                        .collect { result ->
                            result.onSuccess { todos ->
                                todos.forEach { (title, description) ->
                                    val todo = TodoItem(
                                        title = title,
                                        description = description,
                                        priority = 1,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    repository.insertTodo(todo)
                                }
                                _state.value = _state.value.copy(
                                    isGenerating = false,
                                    showAiTodoDialog = false
                                )
                                _events.emit(TodoUiEvent.ShowToast("Successfully generated ${todos.size} tasks"))
                            }.onFailure {
                                _state.value = _state.value.copy(isGenerating = false)
                                _events.emit(TodoUiEvent.ShowToast("Failed to generate tasks: ${it.message}"))
                            }
                        }
                }
            }
        }
    }
}
