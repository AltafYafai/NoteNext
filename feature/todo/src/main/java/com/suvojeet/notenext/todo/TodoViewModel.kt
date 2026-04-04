package com.suvojeet.notenext.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.data.TodoRepository
import com.suvojeet.notenext.data.repository.GroqRepository
import com.suvojeet.notenext.data.repository.GroqResult
import com.suvojeet.notenext.data.repository.onFailure
import com.suvojeet.notenext.data.repository.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

    private val _filter = MutableStateFlow<TodoFilter>(TodoFilter.All)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pagedTodos: Flow<PagingData<TodoItem>> = _filter.flatMapLatest { filter ->
        when (filter) {
            is TodoFilter.All -> repository.getPagedTodos()
            is TodoFilter.Active -> repository.getPagedActiveTodos()
            is TodoFilter.Completed -> repository.getPagedCompletedTodos()
        }
    }.cachedIn(viewModelScope)

    private val _events = MutableSharedFlow<TodoUiEvent>()
    val events: SharedFlow<TodoUiEvent> = _events.asSharedFlow()

    init {
        observeTodos()
    }

    private fun observeTodos() {
        combine(
            repository.getActiveCount(),
            repository.getCompletedCount(),
            _filter
        ) { activeCount, completedCount, filter ->
            _state.value = _state.value.copy(
                isLoading = false,
                activeCount = activeCount,
                completedCount = completedCount,
                filter = filter
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
                _filter.value = event.filter
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
                    try {
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
                                }.onFailure { failure ->
                                    val errorMessage = when (failure) {
                                        is GroqResult.RateLimited -> "AI is busy. Please try again in ${failure.retryAfterSeconds}s."
                                        is GroqResult.InvalidKey -> "Invalid API key. Check settings."
                                        is GroqResult.NetworkError -> "Network error: ${failure.message}"
                                        is GroqResult.AllModelsFailed -> "AI failed to respond. Try again later."
                                        else -> "Failed to generate tasks."
                                    }
                                    _state.value = _state.value.copy(isGenerating = false)
                                    _events.emit(TodoUiEvent.ShowToast(errorMessage))
                                }
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        e.printStackTrace()
                        _state.value = _state.value.copy(isGenerating = false)
                    }
                }
            }
        }
    }
}
