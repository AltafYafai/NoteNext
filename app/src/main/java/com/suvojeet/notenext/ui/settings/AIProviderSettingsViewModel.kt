package com.suvojeet.notenext.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.ai.AIProvider
import com.suvojeet.notenext.data.ai.AIProviderManager
import com.suvojeet.notenext.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIProviderSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiProviderManager: AIProviderManager
) : ViewModel() {

    private val _selectedProvider = MutableStateFlow<AIProvider>(AIProvider.GROQ)
    val selectedProvider: StateFlow<AIProvider> = _selectedProvider.asStateFlow()

    private val _groqKey = MutableStateFlow("")
    val groqKey: StateFlow<String> = _groqKey.asStateFlow()

    private val _openaiApiKey = MutableStateFlow("")
    val openaiApiKey: StateFlow<String> = _openaiApiKey.asStateFlow()

    private val _openaiBaseUrl = MutableStateFlow("https://api.openai.com/")
    val openaiBaseUrl: StateFlow<String> = _openaiBaseUrl.asStateFlow()

    private val _anthropicApiKey = MutableStateFlow("")
    val anthropicApiKey: StateFlow<String> = _anthropicApiKey.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedProvider.value = aiProviderManager.getActiveProvider()
            _groqKey.value = settingsRepository.customGroqKey.first()
            _openaiApiKey.value = settingsRepository.openAIApiKey.first()
            _openaiBaseUrl.value = settingsRepository.openAIBaseUrl.first()
            _anthropicApiKey.value = settingsRepository.anthropicApiKey.first()
        }
    }

    fun selectProvider(provider: AIProvider) {
        viewModelScope.launch {
            _selectedProvider.value = provider
            aiProviderManager.setActiveProvider(provider)
            settingsRepository.savePreferredAIProvider(provider.name)
        }
    }

    fun saveGroqKey(key: String) {
        viewModelScope.launch {
            _groqKey.value = key
            settingsRepository.saveCustomGroqKey(key)
            if (key.isNotBlank()) {
                settingsRepository.saveUseCustomGroqKey(true)
            }
        }
    }

    fun saveOpenaiKey(key: String) {
        viewModelScope.launch {
            _openaiApiKey.value = key
            settingsRepository.saveOpenAIApiKey(key)
        }
    }

    fun saveOpenaiBaseUrl(url: String) {
        viewModelScope.launch {
            _openaiBaseUrl.value = url
            settingsRepository.saveOpenAIBaseUrl(url)
        }
    }

    fun saveAnthropicKey(key: String) {
        viewModelScope.launch {
            _anthropicApiKey.value = key
            settingsRepository.saveAnthropicApiKey(key)
        }
    }
}
