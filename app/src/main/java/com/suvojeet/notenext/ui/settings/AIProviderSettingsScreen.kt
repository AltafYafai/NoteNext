@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.data.ai.AIProvider
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun AIProviderSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: AIProviderSettingsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle(initialValue = AIProvider.GROQ)
    val groqKey by viewModel.groqKey.collectAsStateWithLifecycle(initialValue = "")
    val openaiKey by viewModel.openaiApiKey.collectAsStateWithLifecycle(initialValue = "")
    val openaiBaseUrl by viewModel.openaiBaseUrl.collectAsStateWithLifecycle(initialValue = "https://api.openai.com/")
    val anthropicKey by viewModel.anthropicApiKey.collectAsStateWithLifecycle(initialValue = "")

    var showGroqKeyVisible by remember { mutableStateOf(false) }
    var showOpenaiKeyVisible by remember { mutableStateOf(false) }
    var showAnthropicKeyVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "AI Providers",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ExpressiveSection(
                    title = "Select AI Provider",
                    description = "Choose your preferred AI service for summarization, checklists, and more"
                ) {
                    SettingsGroupCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AIProviderCard(
                                provider = AIProvider.GROQ,
                                name = "Groq",
                                description = "Fast inference with Llama & Qwen models",
                                icon = Icons.Rounded.Bolt,
                                iconColor = Color(0xFFFFC107),
                                isSelected = selectedProvider == AIProvider.GROQ,
                                onClick = { viewModel.selectProvider(AIProvider.GROQ) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            AIProviderCard(
                                provider = AIProvider.OPENAI,
                                name = "OpenAI",
                                description = "GPT-4o and GPT-4 Turbo models",
                                icon = Icons.Rounded.AutoAwesome,
                                iconColor = Color(0xFF10A37A),
                                isSelected = selectedProvider == AIProvider.OPENAI,
                                onClick = { viewModel.selectProvider(AIProvider.OPENAI) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            AIProviderCard(
                                provider = AIProvider.ANTHROPIC,
                                name = "Anthropic (Claude)",
                                description = "Claude 3.5 Sonnet and Opus models",
                                icon = Icons.Rounded.Psychology,
                                iconColor = Color(0xFFFF5722),
                                isSelected = selectedProvider == AIProvider.ANTHROPIC,
                                onClick = { viewModel.selectProvider(AIProvider.ANTHROPIC) }
                            )
                        }
                    }
                }
            }

            // Groq Configuration
            if (selectedProvider == AIProvider.GROQ) {
                item {
                    ExpressiveSection(
                        title = "Groq Configuration",
                        description = "Configure your Groq API key (optional)"
                    ) {
                        SettingsGroupCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = groqKey,
                                    onValueChange = { viewModel.saveGroqKey(it) },
                                    label = { Text("Groq API Key (Optional)") },
                                    placeholder = { Text("gsk_...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (showGroqKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showGroqKeyVisible = !showGroqKeyVisible }) {
                                            Icon(
                                                imageVector = if (showGroqKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Leave empty to use the app's built-in key. Get your key at console.groq.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // OpenAI Configuration
            if (selectedProvider == AIProvider.OPENAI) {
                item {
                    ExpressiveSection(
                        title = "OpenAI Configuration",
                        description = "Enter your OpenAI API key"
                    ) {
                        SettingsGroupCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = openaiBaseUrl,
                                    onValueChange = { viewModel.saveOpenaiBaseUrl(it) },
                                    label = { Text("Base URL (Optional)") },
                                    placeholder = { Text("https://api.openai.com/") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = openaiKey,
                                    onValueChange = { viewModel.saveOpenaiKey(it) },
                                    label = { Text("OpenAI API Key") },
                                    placeholder = { Text("sk-...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (showOpenaiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showOpenaiKeyVisible = !showOpenaiKeyVisible }) {
                                            Icon(
                                                imageVector = if (showOpenaiKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Your API key is stored locally and never sent to any server except OpenAI",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Anthropic Configuration
            if (selectedProvider == AIProvider.ANTHROPIC) {
                item {
                    ExpressiveSection(
                        title = "Anthropic Configuration",
                        description = "Enter your Anthropic API key"
                    ) {
                        SettingsGroupCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = anthropicKey,
                                    onValueChange = { viewModel.saveAnthropicKey(it) },
                                    label = { Text("Anthropic API Key") },
                                    placeholder = { Text("sk-ant-...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (showAnthropicKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showAnthropicKeyVisible = !showAnthropicKeyVisible }) {
                                            Icon(
                                                imageVector = if (showAnthropicKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Your API key is stored locally and never sent to any server except Anthropic",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AIProviderCard(
    provider: AIProvider,
    name: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .springPress(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
