package com.suvojeet.notenext.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.ui.theme.Motion
import com.suvojeet.notenext.ui.theme.HeroShapes

@Composable
fun MultiActionFab(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNoteClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onProjectClick: () -> Unit,
    onScanQrClick: () -> Unit = {},
    onTodoClick: () -> Unit = {},
    showProjectButton: Boolean = true,
    themeMode: ThemeMode,
    isScrollExpanded: Boolean = true
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = Motion.emphasis(),
        label = "FabIconRotation"
    )

    var showProject by remember { mutableStateOf(false) }
    var showTodo by remember { mutableStateOf(false) }
    var showScanQr by remember { mutableStateOf(false) }
    var showChecklist by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            showNote = true
            kotlinx.coroutines.delay(40)
            showChecklist = true
            kotlinx.coroutines.delay(40)
            showTodo = true
            kotlinx.coroutines.delay(40)
            showScanQr = true
            kotlinx.coroutines.delay(40)
            showProject = true
        } else {
            showProject = false
            kotlinx.coroutines.delay(30)
            showScanQr = false
            kotlinx.coroutines.delay(30)
            showTodo = false
            kotlinx.coroutines.delay(30)
            showChecklist = false
            kotlinx.coroutines.delay(30)
            showNote = false
        }
    }

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = Motion.snappy(),
        label = "MainFabPressScale"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Items in reverse order (top to bottom)
        AnimatedVisibility(
            visible = showProject && showProjectButton,
            enter = fadeIn(Motion.emphasis()) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = Motion.emphasis()) + scaleIn(initialScale = 0.8f, animationSpec = Motion.emphasis()),
            exit = fadeOut(Motion.snappy()) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = Motion.snappy()) + scaleOut(targetScale = 0.8f, animationSpec = Motion.snappy())
        ) {
            FabItem(
                icon = Icons.Default.CreateNewFolder,
                label = stringResource(id = R.string.projects),
                onClick = {
                    onProjectClick()
                    onExpandedChange(false)
                },
                themeMode = themeMode
            )
        }

        AnimatedVisibility(
            visible = showScanQr,
            enter = fadeIn(Motion.emphasis()) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = Motion.emphasis()) + scaleIn(initialScale = 0.8f, animationSpec = Motion.emphasis()),
            exit = fadeOut(Motion.snappy()) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = Motion.snappy()) + scaleOut(targetScale = 0.8f, animationSpec = Motion.snappy())
        ) {
            FabItem(
                icon = Icons.Default.QrCodeScanner,
                label = stringResource(id = R.string.scan_qr),
                onClick = {
                    onScanQrClick()
                    onExpandedChange(false)
                },
                themeMode = themeMode
            )
        }

        AnimatedVisibility(
            visible = showTodo,
            enter = fadeIn(Motion.emphasis()) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = Motion.emphasis()) + scaleIn(initialScale = 0.8f, animationSpec = Motion.emphasis()),
            exit = fadeOut(Motion.snappy()) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = Motion.snappy()) + scaleOut(targetScale = 0.8f, animationSpec = Motion.snappy())
        ) {
            FabItem(
                icon = Icons.Default.TaskAlt,
                label = stringResource(id = R.string.todo),
                onClick = {
                    onTodoClick()
                    onExpandedChange(false)
                },
                themeMode = themeMode
            )
        }

        AnimatedVisibility(
            visible = showChecklist,
            enter = fadeIn(Motion.emphasis()) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = Motion.emphasis()) + scaleIn(initialScale = 0.8f, animationSpec = Motion.emphasis()),
            exit = fadeOut(Motion.snappy()) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = Motion.snappy()) + scaleOut(targetScale = 0.8f, animationSpec = Motion.snappy())
        ) {
            FabItem(
                icon = Icons.Default.CheckBox,
                label = stringResource(id = R.string.checklist),
                onClick = {
                    onChecklistClick()
                    onExpandedChange(false)
                },
                themeMode = themeMode
            )
        }

        AnimatedVisibility(
            visible = showNote,
            enter = fadeIn(Motion.emphasis()) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = Motion.emphasis()) + scaleIn(initialScale = 0.8f, animationSpec = Motion.emphasis()),
            exit = fadeOut(Motion.snappy()) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = Motion.snappy()) + scaleOut(targetScale = 0.8f, animationSpec = Motion.snappy())
        ) {
            FabItem(
                icon = Icons.Default.Note,
                label = stringResource(id = R.string.note),
                onClick = {
                    onNoteClick()
                    onExpandedChange(false)
                },
                themeMode = themeMode
            )
        }

        // Main FAB
        androidx.compose.material3.ExtendedFloatingActionButton(
            text = { 
                 AnimatedVisibility(
                     visible = isScrollExpanded && !isExpanded,
                     enter = fadeIn() + expandHorizontally(),
                     exit = fadeOut() + shrinkHorizontally()
                 ) {
                     Text(
                         text = stringResource(id = R.string.add),
                         style = MaterialTheme.typography.labelLarge,
                         fontWeight = FontWeight.ExtraBold
                     )
                 }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add),
                    modifier = Modifier.rotate(rotation)
                )
            },
            onClick = {
                pressed = true
                onExpandedChange(!isExpanded)
            },
            expanded = isScrollExpanded && !isExpanded,
            containerColor = if (isExpanded) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primary,
            contentColor = if (isExpanded) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.scale(pressScale),
            shape = if (isExpanded) HeroShapes.Squircle else MaterialTheme.shapes.extraLarge
        )
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}

@Composable
private fun FabItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    themeMode: ThemeMode
) {
    val cardColor = if (themeMode == ThemeMode.AMOLED) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    
    val border = if (themeMode == ThemeMode.AMOLED) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    } else {
        null
    }

    Card(
        modifier = Modifier.clickable { onClick() },
        shape = HeroShapes.Leaf,
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = if (themeMode == ThemeMode.AMOLED) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = label, 
                tint = if (themeMode == ThemeMode.AMOLED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
