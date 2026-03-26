package com.suvojeet.notenext.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.suvojeet.notenext.MainActivity
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.util.HtmlConverter
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.map

class NoteGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.repository()

        provideContent {
            val notes by repository.getPinnedNotes().map { allPinned ->
                allPinned.filter { !it.note.isArchived && !it.note.isBinned && !it.note.isLocked }
            }.collectAsState(initial = emptyList())

            WidgetContent(context, notes)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, notes: List<NoteWithAttachments>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.glance.material3.ColorProviders.backgroundColor))
                .padding(8.dp)
        ) {
            Header(context)
            if (notes.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No pinned notes", 
                        style = TextStyle(color = ColorProvider(androidx.glance.material3.ColorProviders.onBackgroundColor))
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(notes) { noteWithAttachments ->
                        NoteItem(context, noteWithAttachments)
                    }
                }
            }
        }
    }

    @Composable
    private fun Header(context: Context) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.SpaceBetween
        ) {
            Text(
                text = "Pinned Notes",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ColorProvider(androidx.glance.material3.ColorProviders.onBackgroundColor)
                )
            )
            val addIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("START_ADD_NOTE", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            Text(
                text = "+",
                modifier = GlanceModifier.clickable(actionStartActivity(addIntent)),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = ColorProvider(androidx.glance.material3.ColorProviders.primaryColor)
                )
            )
        }
    }

    @Composable
    private fun NoteItem(context: Context, noteWithAttachments: NoteWithAttachments) {
        val note = noteWithAttachments.note
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("NOTE_ID", note.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(ColorProvider(androidx.glance.material3.ColorProviders.surfaceColor))
                .padding(8.dp)
                .clickable(actionStartActivity(intent))
        ) {
            Text(
                text = note.title.ifEmpty { "Untitled" },
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = ColorProvider(androidx.glance.material3.ColorProviders.onSurfaceColor)
                )
            )
            
            if (note.noteType == NoteType.CHECKLIST) {
                ChecklistPreview(noteWithAttachments.checklistItems)
            } else {
                val plainContent = HtmlConverter.htmlToPlainText(note.content)
                Text(
                    text = plainContent,
                    maxLines = 3,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(androidx.glance.material3.ColorProviders.onSurfaceVariantColor)
                    )
                )
            }
        }
    }

    @Composable
    private fun ChecklistPreview(items: List<ChecklistItem>) {
        Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
            items.sortedBy { it.position }.take(3).forEach { item ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.glance.appwidget.CheckBox(
                        checked = item.isChecked,
                        onCheckedChange = actionRunCallback<ToggleChecklistItemAction>(
                            actionParametersOf(
                                ChecklistItemIdKey to item.id,
                                ChecklistItemCheckedKey to !item.isChecked
                            )
                        )
                    )
                    Text(
                        text = item.text,
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(androidx.glance.material3.ColorProviders.onSurfaceVariantColor)
                        ),
                        modifier = GlanceModifier.padding(start = 4.dp)
                    )
                }
            }
            if (items.size > 3) {
                Text(
                    text = "... and ${items.size - 3} more",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(androidx.glance.material3.ColorProviders.onSurfaceVariantColor)
                    ),
                    modifier = GlanceModifier.padding(start = 28.dp)
                )
            }
        }
    }

    companion object {
        val ChecklistItemIdKey = ActionParameters.Key<String>("checklist_item_id")
        val ChecklistItemCheckedKey = ActionParameters.Key<Boolean>("checklist_item_checked")
    }
}

class ToggleChecklistItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[NoteGlanceWidget.ChecklistItemIdKey] ?: return
        val isChecked = parameters[NoteGlanceWidget.ChecklistItemCheckedKey] ?: return

        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.repository()

        repository.updateChecklistItemStatus(itemId, isChecked)
        
        // Update the widget
        NoteGlanceWidget().update(context, glanceId)
    }
}
