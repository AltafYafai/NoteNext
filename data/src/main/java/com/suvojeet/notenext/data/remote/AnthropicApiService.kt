package com.suvojeet.notenext.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AnthropicApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") apiVersion: String = "2023-06-01",
        @Body request: AnthropicMessageRequest
    ): AnthropicMessageResponse
}

data class AnthropicMessageRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val max_tokens: Int = 4096,
    val system: String? = null,
    val temperature: Double = 0.7
)

data class AnthropicMessage(
    val role: String,
    val content: String
)

data class AnthropicMessageResponse(
    val id: String?,
    val type: String?,
    val role: String?,
    val content: List<AnthropicContentBlock>?,
    val model: String?,
    val stop_reason: String?,
    val error: AnthropicError?
)

data class AnthropicContentBlock(
    val type: String?,
    val text: String?
)

data class AnthropicError(
    val type: String?,
    val message: String?
)
