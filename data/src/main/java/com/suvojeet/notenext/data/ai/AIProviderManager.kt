package com.suvojeet.notenext.data.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages multiple AI providers and routes requests to the appropriate one
 */
@Singleton
class AIProviderManager @Inject constructor(
    private val groqProvider: GroqProvider,
    private val openAIProvider: OpenAIProvider,
    private val anthropicProvider: AnthropicProvider,
    private val settingsRepository: com.suvojeet.notenext.data.repository.SettingsRepository
) {
    private val mutex = Mutex()
    private val _activeProvider = MutableStateFlow<AIProvider>(AIProvider.GROQ)
    val activeProvider: Flow<AIProvider> = _activeProvider

    private val providers = mutableMapOf<AIProvider, AIProviderService>()

    init {
        providers[AIProvider.GROQ] = groqProvider
        providers[AIProvider.OPENAI] = openAIProvider
        providers[AIProvider.ANTHROPIC] = anthropicProvider
    }

    suspend fun setActiveProvider(provider: AIProvider) {
        mutex.withLock {
            settingsRepository.savePreferredAIProvider(provider.name)
            _activeProvider.value = provider
        }
    }

    suspend fun getActiveProvider(): AIProvider {
        val providerName = settingsRepository.preferredAIProvider.first()
        return try {
            AIProvider.valueOf(providerName)
        } catch (e: Exception) {
            AIProvider.GROQ
        }
    }

    suspend fun getProviderService(provider: AIProvider): AIProviderService? {
        return providers[provider]
    }

    suspend fun summarizeNote(content: String): AIResult<String> {
        val provider = getActiveProvider()
        val service = providers[provider] ?: return AIResult.ProviderError("Provider not available: $provider")

        return service.summarizeNote(content)
    }

    suspend fun generateChecklist(topic: String): AIResult<List<String>> {
        val provider = getActiveProvider()
        val service = providers[provider] ?: return AIResult.ProviderError("Provider not available: $provider")

        return service.generateChecklist(topic)
    }

    suspend fun generateTodos(input: String): AIResult<List<Pair<String, String>>> {
        val provider = getActiveProvider()
        val service = providers[provider] ?: return AIResult.ProviderError("Provider not available: $provider")

        return service.generateTodos(input)
    }

    suspend fun fixGrammar(text: String): AIResult<String> {
        val provider = getActiveProvider()
        val service = providers[provider] ?: return AIResult.ProviderError("Provider not available: $provider")

        return service.fixGrammar(text)
    }

    suspend fun generateCustomPrompt(systemPrompt: String, userPrompt: String): AIResult<String> {
        val provider = getActiveProvider()
        val service = providers[provider] ?: return AIResult.ProviderError("Provider not available: $provider")

        return service.generateCustomPrompt(systemPrompt, userPrompt)
    }

    suspend fun isProviderAvailable(provider: AIProvider): Boolean {
        val service = providers[provider] ?: return false
        return service.isProviderAvailable()
    }

    suspend fun getAllAvailableProviders(): List<AIProvider> {
        return providers.keys.filter { providers[it]?.isProviderAvailable() == true }
    }
}
