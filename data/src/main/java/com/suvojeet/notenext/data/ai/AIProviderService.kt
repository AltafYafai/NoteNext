package com.suvojeet.notenext.data.ai

/**
 * Supported AI providers
 */
enum class AIProvider {
    GROQ,
    OPENAI,
    ANTHROPIC,
    OLLAMA,
    CUSTOM
}

/**
 * Abstract interface for all AI providers
 */
interface AIProviderService {
    suspend fun summarizeNote(content: String): AIResult<String>
    suspend fun generateChecklist(topic: String): AIResult<List<String>>
    suspend fun generateTodos(input: String): AIResult<List<Pair<String, String>>>
    suspend fun fixGrammar(text: String): AIResult<String>
    suspend fun generateCustomPrompt(systemPrompt: String, userPrompt: String): AIResult<String>
    suspend fun isProviderAvailable(): Boolean
    suspend fun getProviderName(): String
    suspend fun getAvailableModels(): List<String>
}

/**
 * Unified result type for all AI operations
 */
sealed class AIResult<out T> {
    data class Success<T>(val data: T) : AIResult<T>()
    data class RateLimited(val retryAfterSeconds: Int) : AIResult<Nothing>()
    data class AuthError(val message: String) : AIResult<Nothing>()
    data class NetworkError(val message: String) : AIResult<Nothing>()
    data class ProviderError(val message: String) : AIResult<Nothing>()
    object AllProvidersFailed : AIResult<Nothing>()
}

inline fun <T> AIResult<T>.onSuccess(action: (T) -> Unit): AIResult<T> {
    if (this is AIResult.Success) action(data)
    return this
}

inline fun <T> AIResult<T>.onFailure(action: (AIResult<Nothing>) -> Unit): AIResult<T> {
    if (this !is AIResult.Success) action(this as AIResult<Nothing>)
    return this
}

/**
 * Configuration for an AI provider
 */
data class AIProviderConfig(
    val provider: AIProvider,
    val apiKey: String = "",
    val baseUrl: String = "",
    val models: List<String> = emptyList(),
    val timeout: Int = 30,
    val isEnabled: Boolean = true
)
