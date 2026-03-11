package com.suvojeet.notenext.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.util.HtmlConverter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class NoteWidgetService : RemoteViewsService() {
    @Inject
    lateinit var repository: NoteRepository

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetRemoteViewsFactory(this.applicationContext, repository)
    }
}

class NoteWidgetRemoteViewsFactory(
    private val context: Context,
    private val repository: NoteRepository
) : RemoteViewsService.RemoteViewsFactory {

    private var notes: List<Note> = emptyList()
    private var noteContents: Map<Int, String> = emptyMap()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            try {
                // Fetch Pinned notes first
                val allNotesWithAttachments = repository.getNotes("", com.suvojeet.notenext.data.SortType.DATE_MODIFIED).first()
                notes = allNotesWithAttachments
                        .map { it.note }
                        .filter { it.isPinned && !it.isArchived && !it.isBinned }
                
                // Pre-compute plain text content for all notes to avoid runBlocking in getViewAt
                noteContents = notes.associate { note ->
                    val content = if (note.noteType == "CHECKLIST") {
                        "Checklist..."
                    } else {
                        HtmlConverter.htmlToPlainText(note.content)
                    }
                    note.id to content
                }
            } catch (e: Exception) {
                e.printStackTrace()
                notes = emptyList()
                noteContents = emptyMap()
            }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = notes.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position == -1 || position >= notes.size) return RemoteViews(context.packageName, R.layout.widget_note_item)

        val note = notes[position]
        val views = RemoteViews(context.packageName, R.layout.widget_note_item)

        views.setTextViewText(R.id.widget_item_title, note.title.ifEmpty { "Untitled" })
        
        val plainContent = noteContents[note.id] ?: ""
        views.setTextViewText(R.id.widget_item_content, plainContent)

        val fillInIntent = Intent().apply {
            putExtra("NOTE_ID", note.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = notes[position].id.toLong()
    override fun hasStableIds(): Boolean = true
}