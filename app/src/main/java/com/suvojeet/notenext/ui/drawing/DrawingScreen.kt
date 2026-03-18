package com.suvojeet.notenext.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    onSave: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val paths = remember { mutableStateListOf<DrawingPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(10f) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val colors = listOf(
        Color.Black, Color.Red, Color.Green, Color.Blue, 
        Color.Yellow, Color.Magenta, Color.Cyan, Color.White
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drawing") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (paths.isNotEmpty()) {
                                paths.removeLast()
                            }
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { paths.clear() },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear All")
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val uri = saveDrawingToCache(context, paths)
                                if (uri != null) {
                                    onSave(uri)
                                }
                            }
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, shape = MaterialTheme.shapes.small)
                                .clickable { currentColor = color }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply {
                                    moveTo(offset.x, offset.y)
                                }
                            },
                            onDrag = { change, _ ->
                                currentPath?.lineTo(change.position.x, change.position.y)
                                // Trigger recomposition
                                currentPath = Path().apply { addPath(currentPath!!) }
                            },
                            onDragEnd = {
                                currentPath?.let {
                                    paths.add(DrawingPath(it, currentColor, currentStrokeWidth))
                                }
                                currentPath = null
                            },
                            onDragCancel = {
                                currentPath = null
                            }
                        )
                    }
            ) {
                paths.forEach { drawingPath ->
                    drawPath(
                        path = drawingPath.path,
                        color = drawingPath.color,
                        style = Stroke(
                            width = drawingPath.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                currentPath?.let {
                    drawPath(
                        path = it,
                        color = currentColor,
                        style = Stroke(
                            width = currentStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

private suspend fun saveDrawingToCache(context: Context, paths: List<DrawingPath>): Uri? = withContext(Dispatchers.IO) {
    try {
        // Create an empty bitmap matching a typical screen size or fixed size
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }

        paths.forEach { drawingPath ->
            paint.color = drawingPath.color.toArgb()
            paint.strokeWidth = drawingPath.strokeWidth
            
            // To convert Compose Path to Android Path
            val androidPath = drawingPath.path.asAndroidPath()
            canvas.drawPath(androidPath, paint)
        }

        val file = File(context.cacheDir, "drawing_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
