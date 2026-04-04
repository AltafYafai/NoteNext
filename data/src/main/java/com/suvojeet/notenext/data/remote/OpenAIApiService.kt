package com.suvojeet.notenext.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAIApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") auth: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse
}

data class OpenAIChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null
)

data class OpenAIChatResponse(
    val id: String?,
    val `object`: String?,
    val created: Long?,
    val model: String?,
    val choices: List<OpenAIChoice>?,
    val usage: OpenAIUsage?,
    val error: OpenAIError?
)

data class OpenAIChoice(
    val index: Int?,
    val message: Message?,
    val finish_reason: String?
)

data class OpenAIUsage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)

data class OpenAIError(
    val message: String?,
    val type: String?,
    val code: String?
)
