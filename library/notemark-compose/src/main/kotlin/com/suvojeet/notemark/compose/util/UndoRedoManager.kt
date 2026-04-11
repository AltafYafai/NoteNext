package com.suvojeet.notemark.compose.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A generic manager for handling undo/redo operations in a text editor or similar component.
 */
class UndoRedoManager<T>(initialState: T, private val maxHistorySize: Int = 50) {

    private val history = mutableListOf<T>()
    private var currentIndex = 0

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    init {
        history.add(initialState)
        updateFlags()
    }

    /**
     * Adds a new state to the history, clearing any redo history.
     */
    fun addState(state: T) {
        // If the new state is same as current, don't add
        if (state == history[currentIndex]) return

        // If we are not at the end, remove all future states (redo history)
        if (currentIndex < history.size - 1) {
            val itemsToRemove = history.size - 1 - currentIndex
            repeat(itemsToRemove) {
                history.removeAt(history.size - 1)
            }
        }

        history.add(state)
        
        // Limit history size
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        } else {
            currentIndex++
        }
        
        // Ensure currentIndex is correct if we removed items
        currentIndex = history.size - 1

        updateFlags()
    }

    /**
     * Reverts to the previous state.
     */
    fun undo(): T? {
        if (currentIndex > 0) {
            currentIndex--
            updateFlags()
            return history[currentIndex]
        }
        return null
    }

    /**
     * Proceeds to the next state in the redo history.
     */
    fun redo(): T? {
        if (currentIndex < history.size - 1) {
            currentIndex++
            updateFlags()
            return history[currentIndex]
        }
        return null
    }

    /**
     * Returns the current state.
     */
    fun getCurrentState(): T {
        return history[currentIndex]
    }
    
    /**
     * Resets the history with a new initial state.
     */
    fun reset(state: T) {
        history.clear()
        history.add(state)
        currentIndex = 0
        updateFlags()
    }

    private fun updateFlags() {
        _canUndo.value = currentIndex > 0
        _canRedo.value = currentIndex < history.size - 1
    }
}
