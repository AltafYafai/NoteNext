package com.suvojeet.notemark.core.util

/**
 * Interface representing a checklist item that can be managed by [BaseChecklistManager].
 */
interface ChecklistItemLike<T : ChecklistItemLike<T>> {
    val id: String
    val text: String
    val isChecked: Boolean
    val position: Int
    val level: Int

    fun copyWith(
        text: String = this.text,
        isChecked: Boolean = this.isChecked,
        position: Int = this.position,
        level: Int = this.level
    ): T
}

/**
 * A generic manager for checklist logic, decoupled from specific data models.
 */
abstract class BaseChecklistManager<T : ChecklistItemLike<T>> {

    /**
     * Factory method to create a new checklist item.
     */
    abstract fun createNewItem(position: Int): T

    fun addChecklistItem(currentList: List<T>): Pair<List<T>, String> {
        val newItem = createNewItem(currentList.size)
        return (currentList + newItem) to newItem.id
    }

    fun swapItems(currentList: List<T>, fromId: String, toId: String): List<T> {
        val list = currentList.toMutableList()
        val fromIndex = list.indexOfFirst { it.id == fromId }
        val toIndex = list.indexOfFirst { it.id == toId }

        if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
            val fromItem = list[fromIndex]
            val toItem = list[toIndex]
            list[fromIndex] = toItem.copyWith(position = fromIndex)
            list[toIndex] = fromItem.copyWith(position = toIndex)
            
            // Re-normalize positions just in case
            return list.mapIndexed { index, item -> item.copyWith(position = index) }
        }
        return currentList
    }

    fun deleteItem(currentList: List<T>, itemId: String): List<T> {
        return currentList.filterNot { it.id == itemId }
    }

    fun indentItem(currentList: List<T>, itemId: String): List<T> {
        return currentList.map {
            if (it.id == itemId) it.copyWith(level = (it.level + 1).coerceAtMost(5)) else it
        }
    }

    fun outdentItem(currentList: List<T>, itemId: String): List<T> {
        return currentList.map {
            if (it.id == itemId) it.copyWith(level = (it.level - 1).coerceAtLeast(0)) else it
        }
    }

    fun changeItemCheckedState(currentList: List<T>, itemId: String, isChecked: Boolean): List<T> {
        val updatedChecklist = currentList.toMutableList()
        val index = updatedChecklist.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = updatedChecklist.removeAt(index).copyWith(isChecked = isChecked)
            if (isChecked) {
                updatedChecklist.add(item) // Add to the end if checked
            } else {
                // Find the first checked item and insert before it, or at the end if no checked items
                val firstCheckedIndex = updatedChecklist.indexOfFirst { it.isChecked }
                if (firstCheckedIndex != -1) {
                    updatedChecklist.add(firstCheckedIndex, item)
                } else {
                    // Try to restore original position or just add to the beginning
                    updatedChecklist.add(0, item)
                }
            }
        }
        // Re-normalize positions after move
        return updatedChecklist.mapIndexed { i, it -> it.copyWith(position = i) }
    }

    fun changeItemText(currentList: List<T>, itemId: String, text: String): List<T> {
        return currentList.map {
            if (it.id == itemId) it.copyWith(text = text) else it
        }
    }

    fun deleteAllCheckedItems(currentList: List<T>): List<T> {
        return currentList.filterNot { it.isChecked }
    }
}
