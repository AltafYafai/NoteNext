@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.drawing

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
import androidx.compose.material.icons.automirrored.filled.Redo
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import kotlinx.coroutines.launch

@Composable
fun DrawingScreen(
    onSave: (Uri) -> Unit,
    onDismiss: () -> Unit,
    viewModel: DrawingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Local state for the current stroke being drawn to avoid ViewModel overhead during fast drags
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
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
                        onClick = { viewModel.onEvent(DrawingEvent.Undo) },
                        enabled = state.paths.isNotEmpty(),
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.onEvent(DrawingEvent.Redo) },
                        enabled = state.undonePaths.isNotEmpty(),
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(
                        onClick = { viewModel.onEvent(DrawingEvent.ClearAll) },
                        enabled = state.paths.isNotEmpty(),
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                    }
                    
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Button(
                            onClick = {
                                viewModel.onEvent(DrawingEvent.SaveDrawing(context) { uri ->
                                    if (uri != null) onSave(uri)
                                })
                            },
                            enabled = state.paths.isNotEmpty(),
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
                        visible = state.showBrushSettings,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Slider(
                                    value = state.currentStrokeWidth,
                                    onValueChange = { viewModel.onEvent(DrawingEvent.ChangeStrokeWidth(it)) },
                                    valueRange = 5f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "${state.currentStrokeWidth.toInt()}px",
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
                                selected = !state.isEraserMode,
                                onClick = { viewModel.onEvent(DrawingEvent.ToggleEraserMode(false)) },
                                shape = CircleShape,
                                color = if (!state.isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.size(48.dp).springPress()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Brush, contentDescription = "Brush")
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                selected = state.isEraserMode,
                                onClick = { viewModel.onEvent(DrawingEvent.ToggleEraserMode(true)) },
                                shape = CircleShape,
                                color = if (state.isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
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
                                            width = if (state.currentColor == color && !state.isEraserMode) 3.dp else 1.dp,
                                            color = if (state.currentColor == color && !state.isEraserMode) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { 
                                            viewModel.onEvent(DrawingEvent.ChangeColor(color))
                                        }
                                        .springPress()
                                )
                            }
                        }

                        // Settings Toggle
                        IconButton(
                            onClick = { viewModel.onEvent(DrawingEvent.ToggleBrushSettings) },
                            modifier = Modifier
                                .background(
                                    if (state.showBrushSettings) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
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
                .background(Color.Transparent) // Changed from White to Transparent for true erasing
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // Use graphicsLayer with CompositingStrategy.Offscreen for true eraser BlendMode to work
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .pointerInput(state.isEraserMode, state.currentColor, state.currentStrokeWidth) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply {
                                    moveTo(offset.x, offset.y)
                                }
                            },
                            onDrag = { change, _ ->
                                // Senior Optimization: Mutate the path directly instead of allocating a new one
                                currentPath?.lineTo(change.position.x, change.position.y)
                                // Force recomposition by re-assigning (compose sees same object, so we wrap)
                                currentPath = Path().apply { addPath(currentPath!!) }
                            },
                            onDragEnd = {
                                currentPath?.let {
                                    viewModel.onEvent(DrawingEvent.PathAdded(it))
                                }
                                currentPath = null
                            },
                            onDragCancel = {
                                currentPath = null
                            }
                        )
                    }
            ) {
                // Draw all saved paths
                state.paths.forEach { drawingPath ->
                    drawPath(
                        path = drawingPath.path,
                        color = drawingPath.color,
                        style = Stroke(
                            width = drawingPath.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        // True eraser uses BlendMode.Clear
                        blendMode = if (drawingPath.isEraser) BlendMode.Clear else BlendMode.SrcOver
                    )
                }
                
                // Draw current stroke
                currentPath?.let {
                    drawPath(
                        path = it,
                        color = if (state.isEraserMode) Color.Transparent else state.currentColor,
                        style = Stroke(
                            width = state.currentStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = if (state.isEraserMode) BlendMode.Clear else BlendMode.SrcOver
                    )
                }
            }
            
            // Helpful Hint
            if (state.paths.isEmpty() && currentPath == null) {
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
