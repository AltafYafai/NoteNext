package com.suvojeet.notenext.ui.notes

import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notemark.core.util.BaseChecklistManager

object ChecklistManager : BaseChecklistManager<ChecklistItem>() {

    override fun createNewItem(position: Int): ChecklistItem {
        return ChecklistItem(text = "", isChecked = false, position = position)
    }
}
