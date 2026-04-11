# NoteMark 📝

**NoteMark** is a high-performance, modular Markdown library for Kotlin and Jetpack Compose. It is designed to be lightweight, extensible, and visually beautiful, following Material 3 design principles.

## 🏗 Modular Architecture

The library is split into two modules to ensure maximum flexibility:

1.  **`notemark-core`**: A pure Kotlin/JVM module. It contains the Lexer, Parser, and Abstract Syntax Tree (AST). It has zero dependencies on Android or UI frameworks.
2.  **`notemark-compose`**: An Android library that provides Jetpack Compose components to render the AST into beautiful UI.

---

## 🚀 Getting Started

### 1. Installation

Add the modules to your `settings.gradle.kts`:

```kotlin
include(":notemark-core")
include(":notemark-compose")
project(":notemark-core").projectDir = file("library/notemark-core")
project(":notemark-compose").projectDir = file("library/notemark-compose")
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
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

### 3. Advanced Usage (Core Parser)

If you only need to parse text (e.g., for background processing or indexing):

```kotlin
import com.suvojeet.notemark.core.NoteMarkParser

val document = NoteMarkParser.parse("# My Header\nThis is a paragraph.")
// Access the AST nodes
document.children.forEach { block ->
    println(block)
}
```

---

## ✨ Supported Features

| Feature | Syntax | Support |
| :--- | :--- | :--- |
| **Headers** | `# H1` ... `###### H6` | ✅ Full |
| **Emphasis** | `**Bold**`, `*Italic*` | ✅ Full |
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
