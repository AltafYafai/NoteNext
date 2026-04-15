
package com.suvojeet.notenext.ui.notes.delegate

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.SortType
import com.suvojeet.notenext.ui.notes.NotesListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class NoteListDelegate @Inject constructor(
    private val repository: NoteRepository
) {
    private val _listState = MutableStateFlow(NotesListState())
    val listState = _listState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED)
    private val _filteredProjectId = MutableStateFlow<Int?>(null)

    fun observeNotes(scope: CoroutineScope) {
        val combinedFlow = combine(
            _searchQuery,
            _sortType,
            _filteredProjectId
        ) { query, sort, project -> Triple(query, sort, project) }
            .distinctUntilChanged()

        combinedFlow.flatMapLatest { (query, _, project) ->
            repository.getPinnedNoteSummaries(query, project)
        }.onEach { pinned ->
            _listState.update { it.copy(pinnedNotes = pinned) }
        }.launchIn(scope)

        combinedFlow.onEach { (query, sort, project) ->
            _listState.update { 
                it.copy(
                    pagedNotes = repository.getOtherNoteSummariesPaged(query, sort, project).cachedIn(scope),
                    searchQuery = query,
                    sortType = sort,
                    filteredProjectId = project
                )
            }
        }.launchIn(scope)

        repository.getLabels().onEach { labels ->
            val labelNames = labels.map { it.name }
            _listState.update { it.copy(labels = labelNames) }
        }.launchIn(scope)

        repository.getProjects().onEach { projects ->
            _listState.update { it.copy(projects = projects, isLoading = false) }
        }.launchIn(scope)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
    }

    fun setProjectId(projectId: Int?) {
        _filteredProjectId.value = projectId
    }

    fun toggleSelection(noteId: Int) {
        _listState.update { state ->
            val selectedIds = state.selectedNoteIds.toMutableList()
            if (selectedIds.contains(noteId)) {
                selectedIds.remove(noteId)
            } else {
                selectedIds.add(noteId)
            }
            state.copy(selectedNoteIds = selectedIds)
        }
    }

    fun clearSelection() {
        _listState.update { it.copy(selectedNoteIds = emptyList()) }
    }
}
