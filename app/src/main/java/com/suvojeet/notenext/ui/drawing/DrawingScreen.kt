@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

@Composable
fun DrawingScreen(
    onSave: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val paths = remember { mutableStateListOf<DrawingPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(10f) }
    var isEraserMode by remember { mutableStateOf(false) }
    
    var showBrushSettings by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val colors = listOf(
        Color.Black, Color(0xFF424242), Color(0xFF757575), Color.White,
        Color(0xFFD32F2F), Color(0xFFC2185B), Color(0xFF7B1FA2), Color(0xFF512DA8),
        Color(0xFF303F9F), Color(0xFF1976D2), Color(0xFF0288D1), Color(0xFF0097A7),
        Color(0xFF00796B), Color(0xFF388E3C), Color(0xFF689F38), Color(0xFFFBC02D),
        Color(0xFFFFA000), Color(0xFFF57C00), Color(0xFFE64A19), Color(0xFF5D4037)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.drawing),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex) },
                        enabled = paths.isNotEmpty(),
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { paths.clear() },
                        enabled = paths.isNotEmpty(),
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                    }
                    
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSaving = true
                                    val uri = saveDrawingToCache(context, paths)
                                    isSaving = false
                                    if (uri != null) onSave(uri)
                                }
                            },
                            enabled = paths.isNotEmpty(),
                            modifier = Modifier.padding(end = 8.dp).springPress(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 10.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AnimatedVisibility(
                        visible = showBrushSettings,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Slider(
                                    value = currentStrokeWidth,
                                    onValueChange = { currentStrokeWidth = it },
                                    valueRange = 5f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "${currentStrokeWidth.toInt()}px",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.width(45.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tool Switchers
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                selected = !isEraserMode,
                                onClick = { isEraserMode = false; isEraserMode = false },
                                shape = CircleShape,
                                color = if (!isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.size(48.dp).springPress()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Brush, contentDescription = "Brush")
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                selected = isEraserMode,
                                onClick = { isEraserMode = true },
                                shape = CircleShape,
                                color = if (isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.size(48.dp).springPress()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = "Eraser")
                                }
                            }
                        }

                        // Color Selection
                        LazyRow(
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(colors) { color ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (currentColor == color && !isEraserMode) 3.dp else 1.dp,
                                            color = if (currentColor == color && !isEraserMode) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { 
                                            currentColor = color
                                            isEraserMode = false
                                        }
                                        .springPress()
                                )
                            }
                        }

                        // Settings Toggle
                        IconButton(
                            onClick = { showBrushSettings = !showBrushSettings },
                            modifier = Modifier
                                .background(
                                    if (showBrushSettings) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    CircleShape
                                )
                                .springPress()
                        ) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Settings")
                        }
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
                    .pointerInput(isEraserMode, currentColor, currentStrokeWidth) {
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
                                    paths.add(DrawingPath(it, if (isEraserMode) Color.White else currentColor, currentStrokeWidth, isEraserMode))
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
                        color = if (isEraserMode) Color.White else currentColor,
                        style = Stroke(
                            width = currentStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            
            // Helpful Hint
            if (paths.isEmpty() && currentPath == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Start drawing here...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.LightGray,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

private suspend fun saveDrawingToCache(context: Context, paths: List<DrawingPath>): Uri? = withContext(Dispatchers.IO) {
    try {
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
