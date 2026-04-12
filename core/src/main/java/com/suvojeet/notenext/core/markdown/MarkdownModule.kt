package com.suvojeet.notenext.core.markdown

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import androidx.compose.runtime.staticCompositionLocalOf
import javax.inject.Singleton

val LocalMarkwon = staticCompositionLocalOf<Markwon?> {
    null
}

@Module
@InstallIn(SingletonComponent::class)
object MarkdownModule {

    @Provides
    @Singleton
    fun provideMarkwon(@ApplicationContext context: Context): Markwon {
        return Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMarkdownParser(markwon: Markwon): MarkdownParser {
        return MarkwonParserImpl(markwon)
    }

    @Provides
    @Singleton
    fun provideMarkwonAnnotatedStringBridge(): MarkwonAnnotatedStringBridge {
        return MarkwonAnnotatedStringBridge()
    }
}
