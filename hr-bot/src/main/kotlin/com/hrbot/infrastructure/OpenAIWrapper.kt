package com.hrbot.infrastructure

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.hrbot.domain.AIService
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class OpenAIWrapper(
    private val token: String
) : AIService {
    private val openAI = OpenAI(token)
    private val modelId = ModelId("gpt-5-nano") 

    override suspend fun analyze(prompt: String, content: String): String {
        logger.info { "Sending request to OpenAI with prompt length: ${prompt.length} and content length: ${content.length}" }
        
        val request = ChatCompletionRequest(
            model = modelId,
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = prompt + "\nIMPORTANT: Return ONLY valid JSON."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = content.take(15000)
                )
            )
        )

        val completion = openAI.chatCompletion(request)
        return completion.choices.firstOrNull()?.message?.content ?: "{\"error\": \"Failed to get response from AI\"}"
    }
}
