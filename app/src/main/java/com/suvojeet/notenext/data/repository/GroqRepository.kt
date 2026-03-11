package com.suvojeet.notenext.data.repository

import android.util.LruCache
import com.suvojeet.notenext.data.remote.ChatCompletionRequest
import com.suvojeet.notenext.data.remote.GroqApiService
import com.suvojeet.notenext.data.remote.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqRepository @Inject constructor(
    private val apiService: GroqApiService
) {
    private val json = Json { ignoreUnknownKeys = true }

    // In-memory caches for AI responses to prevent redundant API calls
    private val summaryCache = LruCache<String, String>(50)
    private val checklistCache = LruCache<String, List<String>>(50)
    private val grammarCache = LruCache<String, String>(50)

    private val fastModels = listOf(
        "llama-3.1-8b-instant",
        "gemma2-9b-it",
        "mixtral-8x7b-32768",
        "llama-3.3-70b-versatile",
        "llama-3.1-70b-versatile"
    )

    private val largeModels = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-70b-versatile",
        "mixtral-8x7b-32768",
        "gemma2-9b-it",
        "llama-3.1-8b-instant"
    )

    /**
     * Executes the API request with fallback models and retry logic (exponential backoff).
     */
    private suspend fun <T> executeWithRetry(
        models: List<String>,
        messages: List<Message>,
        maxRetriesPerModel: Int = 2,
        processor: (String) -> T
    ): Result<T> {
        var lastException: Exception? = null

        for (model in models) {
            var currentRetry = 0
            while (currentRetry <= maxRetriesPerModel) {
                try {
                    val request = ChatCompletionRequest(
                        model = model,
                        messages = messages
                    )
                    val response = apiService.getChatCompletion(request)
                    val content = response.choices.firstOrNull()?.message?.content
                    
                    if (content != null) {
                        return Result.success(processor(content))
                    } else {
                        lastException = Exception("Empty response from $model")
                        break // Move to the next model if response is empty
                    }
                } catch (e: Exception) {
                    lastException = e
                    if (e is IOException || e.javaClass.simpleName == "HttpException" && e.message?.contains("50") == true) {
                        // Network error or potentially Server error, worth retrying
                        currentRetry++
                        if (currentRetry <= maxRetriesPerModel) {
                            delay(1000L * currentRetry) // Exponential-like backoff
                        }
                    } else {
                        // Client error (e.g., 401 Unauthorized, 400 Bad Request) or other exceptions, try next model
                        break
                    }
                }
            }
        }

        return Result.failure(lastException ?: Exception("Unknown error during AI request"))
    }

    fun summarizeNote(content: String): Flow<Result<String>> = flow {
        // Return cached result if available
        summaryCache.get(content)?.let {
            emit(Result.success(it))
            return@flow
        }

        // Word count logic to select model list
        val wordCount = content.split("\\s+".toRegex()).size
        val models = if (wordCount < 1000) fastModels else largeModels

        val messages = listOf(
            Message(role = "system", content = "You are a helpful assistant that summarizes notes concisely."),
            Message(role = "user", content = "Summarize the following note:\n\n$content")
        )

        val result = executeWithRetry(models, messages) { it.trim() }
        result.onSuccess { summaryCache.put(content, it) }
        emit(result)
    }

    fun generateChecklist(topic: String): Flow<Result<List<String>>> = flow {
        // Return cached result if available
        checklistCache.get(topic)?.let {
            emit(Result.success(it))
            return@flow
        }

        val messages = listOf(
            Message(
                role = "system", 
                content = "You are a helpful assistant that generates checklists. Return ONLY a pure JSON array of strings, e.g. [\"Item 1\", \"Item 2\"]. Do not include markdown code blocks or any other text."
            ),
            Message(role = "user", content = "Create a checklist for: $topic")
        )

        val result = executeWithRetry(largeModels, messages) { content ->
            val cleaned = content.replace("```json", "").replace("```", "").trim()
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                try {
                    // Try to parse JSON array manually or via Kotlinx Serialization
                    json.decodeFromString(ListSerializer(String.serializer()), cleaned)
                } catch (e: Exception) {
                    // Fallback if JSON parse fails
                    content.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("- ").removePrefix("* ") }
                }
            } else {
                // Fallback if not JSON: split by newlines
                content.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("- ").removePrefix("* ") }
            }
        }
        
        result.onSuccess { checklistCache.put(topic, it) }
        emit(result)
    }

    fun generateTodos(input: String): Flow<Result<List<Pair<String, String>>>> = flow {
        val messages = listOf(
            Message(
                role = "system", 
                content = "You are a helpful assistant that converts paragraphs or messy notes into clear, point-by-point todo tasks. For each task, provide a concise title and a short description if needed. Return ONLY a pure JSON array of objects, each with 'title' and 'description' keys. Example: [{\"title\": \"Buy milk\", \"description\": \"Get full cream milk from store\"}, {\"title\": \"Call mom\", \"description\": \"Wish her happy birthday\"}]. Do not include markdown code blocks or any other text."
            ),
            Message(role = "user", content = "Convert this into a todo list:\n\n$input")
        )

        val result = executeWithRetry(largeModels, messages) { content ->
            val cleaned = content.replace("```json", "").replace("```", "").trim()
            try {
                // Parse JSON array of objects
                val todoList = json.decodeFromString<List<Map<String, String>>>(cleaned)
                todoList.map { 
                    it["title"].orEmpty() to it["description"].orEmpty()
                }
            } catch (e: Exception) {
                // Fallback: split by newlines if JSON fails
                content.lines()
                    .filter { it.isNotBlank() }
                    .map { 
                        val text = it.trim().removePrefix("- ").removePrefix("* ")
                        text to ""
                    }
            }
        }
        emit(result)
    }

    /**
     * Fixes grammar, typos, and punctuation in the given text.
     * Preserves original meaning and formatting.
     */
    fun fixGrammar(text: String): Flow<Result<String>> = flow {
        // Return cached result if available
        grammarCache.get(text)?.let {
            emit(Result.success(it))
            return@flow
        }

        val messages = listOf(
            Message(
                role = "system", 
                content = "You are a grammar and spelling correction assistant. Fix typos, grammar errors, and improve punctuation. Keep the original meaning and tone intact. Return ONLY the corrected text without any explanations or additional comments."
            ),
            Message(role = "user", content = text)
        )

        val result = executeWithRetry(fastModels, messages) { it.trim() }
        result.onSuccess { grammarCache.put(text, it) }
        emit(result)
    }
}
