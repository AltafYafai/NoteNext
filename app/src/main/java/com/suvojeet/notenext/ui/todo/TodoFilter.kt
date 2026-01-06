package com.suvojeet.notenext.ui.todo

sealed class TodoFilter {
    object All : TodoFilter()
    object Active : TodoFilter()
    object Completed : TodoFilter()
}
