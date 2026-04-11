# NoteMark 📝

**NoteMark** is a high-performance, modular Markdown library for Kotlin and Jetpack Compose. It is designed to be lightweight, extensible, and visually beautiful, following Material 3 design principles.

## 🏗 Modular Architecture

The library is split into two modules to ensure maximum flexibility:

1.  **`notemark-core`**: A pure Kotlin/JVM module. It contains the Lexer, Parser, Abstract Syntax Tree (AST), and **Exporter**. It has zero dependencies on Android or UI frameworks.
2.  **`notemark-compose`**: An Android library providing Jetpack Compose components to render the AST and **Editor Utilities** for WYSIWYG-style Markdown editing.

---

## 🚀 Getting Started

### 1. Installation

Add the modules to your `settings.gradle.kts`:

```kotlin
include(":notemark-core")
include(":notemark-compose")
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":notemark-core"))
    implementation(project(":notemark-compose"))
}
```

### 2. Basic Usage (Compose)

The simplest way to render Markdown is using the `MarkdownPreview` component:

```kotlin
import com.suvojeet.notemark.compose.MarkdownPreview

@Composable
fun MyNoteScreen(content: String) {
    MarkdownPreview(
        content = content,
        modifier = Modifier.padding(16.dp),
        onLinkClick = { url ->
            // Handle link navigation
        }
    )
}
```

### 3. Advanced Usage (Editor Utilities)

`NoteMark` provides powerful utilities for building a WYSIWYG Markdown editor using Compose's `TextFieldValue`.

```kotlin
import com.suvojeet.notemark.compose.MarkdownEditorUtils

// Convert Markdown string to Styled AnnotatedString for TextField
val annotatedString = MarkdownEditorUtils.markdownToAnnotatedString("# Hello World")

// Apply Bold to current selection
val result = MarkdownEditorUtils.toggleStyle(
    content = textFieldValue,
    styleToToggle = SpanStyle(fontWeight = FontWeight.Bold),
    currentActiveStyles = activeStyles,
    isBoldActive = isBoldActive,
    isItalicActive = false,
    isUnderlineActive = false
)

// Convert Styled AnnotatedString back to Markdown for saving
val markdown = MarkdownEditorUtils.annotatedStringToMarkdown(textFieldValue.annotatedString)
```

### 4. Core Parser & Exporter

If you only need to work with the Abstract Syntax Tree (AST):

```kotlin
import com.suvojeet.notemark.core.NoteMarkParser
import com.suvojeet.notemark.core.NoteMarkExporter

// Parse Markdown to AST
val document = NoteMarkParser.parse("# Header\nContent")

// Modify AST (e.g., add a node)
// ...

// Export AST back to Markdown string
val markdown = NoteMarkExporter.export(document)
```

---

## ✨ Supported Features

| Feature | Syntax | Support |
| :--- | :--- | :--- |
| **Headers** | `# H1` ... `###### H6` | ✅ Full |
| **Emphasis** | `**Bold**`, `*Italic*` | ✅ Full |
| **Underline** | `__u__Underline__u__` | ✅ Custom |
| **Blockquotes** | `> Quote` | ✅ Basic |
| **Code Blocks** | ` ```kotlin ... ``` ` | ✅ Highlighted Background |
| **Inline Code** | `` `code` `` | ✅ Themed |
| **Links** | `[Title](url)` | ✅ Clickable |
| **WikiLinks** | `[[Internal Link]]` | ✅ Custom Support |
| **Horizontal Rule**| `---` | ✅ Rendered |

---

## 🎨 Customization

You can provide a custom `MarkdownTheme` to match your app's branding:

```kotlin
val myTheme = MarkdownTheme.default().copy(
    colors = MarkdownColors(...),
    typography = MarkdownTypography(...)
)

MarkdownPreview(
    content = text,
    theme = myTheme
)
```

## 🛠 Tech Stack
- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose (Material 3 Expressive)
- **Toolchain**: JDK 21, Android SDK 37
- **Image Loading**: Coil 3.x

---

## 📄 License
This library is part of the NoteNext project. Feel free to modularize and reuse it in your own Kotlin/Android applications.
