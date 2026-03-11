@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun MultiActionFab(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNoteClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onProjectClick: () -> Unit,
    onTodoClick: () -> Unit = {},
    showProjectButton: Boolean = true,
    themeMode: ThemeMode,
    isScrollExpanded: Boolean = true
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "FabIconRotation"
    )

    var showProject by remember { mutableStateOf(false) }
    var showTodo by remember { mutableStateOf(false) }
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
            showProject = true
        } else {
            showProject = false
            kotlinx.coroutines.delay(30)
            showTodo = false
            kotlinx.coroutines.delay(30)
            showChecklist = false
            kotlinx.coroutines.delay(30)
            showNote = false
        }
    }

    val pressScale by animateFloatAsState(
        targetValue = 1f, // Standard M3E buttons should handle their own press
        animationSpec = spring(),
        label = "MainFabPressScale"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val bouncySpringSpec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        val bouncyIntOffsetSpec = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

        // Items in reverse order (top to bottom)
        AnimatedVisibility(
            visible = showProject && showProjectButton,
            enter = fadeIn(bouncySpringSpec) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = bouncyIntOffsetSpec) + scaleIn(initialScale = 0.5f, animationSpec = bouncySpringSpec),
            exit = fadeOut(spring()) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = spring()) + scaleOut(targetScale = 0.8f, animationSpec = spring())
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
            visible = showTodo,
            enter = fadeIn(bouncySpringSpec) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = bouncyIntOffsetSpec) + scaleIn(initialScale = 0.5f, animationSpec = bouncySpringSpec),
            exit = fadeOut(spring()) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = spring()) + scaleOut(targetScale = 0.8f, animationSpec = spring())
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
            enter = fadeIn(bouncySpringSpec) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = bouncyIntOffsetSpec) + scaleIn(initialScale = 0.5f, animationSpec = bouncySpringSpec),
            exit = fadeOut(spring()) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = spring()) + scaleOut(targetScale = 0.8f, animationSpec = spring())
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
            enter = fadeIn(bouncySpringSpec) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = bouncyIntOffsetSpec) + scaleIn(initialScale = 0.5f, animationSpec = bouncySpringSpec),
            exit = fadeOut(spring()) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = spring()) + scaleOut(targetScale = 0.8f, animationSpec = spring())
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
        ExtendedFloatingActionButton(
            text = { 
                 AnimatedVisibility(
                     visible = isScrollExpanded && !isExpanded,
                     enter = fadeIn() + expandHorizontally(),
                     exit = fadeOut() + shrinkHorizontally()
                 ) {
                     Text(
                         text = stringResource(id = R.string.add),
                         style = MaterialTheme.typography.labelLarge,
                         fontWeight = FontWeight.Black
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
                onExpandedChange(!isExpanded)
            },
            expanded = isScrollExpanded && !isExpanded,
            containerColor = if (isExpanded) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primary,
            contentColor = if (isExpanded) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.scale(pressScale).springPress(),
            shape = MaterialTheme.shapes.extraLarge
        )
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
        modifier = Modifier.clickable { onClick() }.springPress(),
        shape = MaterialTheme.shapes.large,
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
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
