package com.suvojeet.notenext.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable
    data class Notes(val noteId: Int = -1) : Destination
    
    @Serializable
    data object Projects : Destination
    
    @Serializable
    data class ProjectNotes(val projectId: Int) : Destination
    
    @Serializable
    data object Archive : Destination
    
    @Serializable
    data object Bin : Destination
    
    @Serializable
    data object Settings : Destination
    
    @Serializable
    data object Backup : Destination
    
    @Serializable
    data object EditLabels : Destination
    
    @Serializable
    data object Reminder : Destination
    
    @Serializable
    data object AddEditReminder : Destination
    
    @Serializable
    data object About : Destination
    
    @Serializable
    data object QrScanner : Destination
    
    @Serializable
    data object Todo : Destination
    
    @Serializable
    data class AddEditNote(val projectId: Int = -1, val noteType: String = "TEXT") : Destination
}
