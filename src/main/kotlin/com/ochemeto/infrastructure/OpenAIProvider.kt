package com.ochemeto.infrastructure

import com.ochemeto.domain.ExtractedContent
import com.ochemeto.domain.Result
import com.ochemeto.domain.SummarizerError
import com.ochemeto.domain.Summary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

interface AIProvider {
    suspend fun generateSummary(content: ExtractedContent): Result<Summary>
}

class OpenAIProvider(
    private val apiKey: String,
    private val httpClient: HttpClient
) : AIProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateSummary(content: ExtractedContent): Result<Summary> {
        val request = ChatCompletionRequest(
            model = MODEL,
            messages = listOf(
                ChatMessage("system", SYSTEM_PROMPT),
                ChatMessage("user", content.text)
            ),
            responseFormat = ResponseFormat("json_object")
        )

        return try {
            val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ChatCompletionResponse>()

            val contentJson = response.choices.firstOrNull()?.message?.content
                ?: return Result.Failure(SummarizerError.AIError("Empty response from OpenAI"))

            val dto = json.decodeFromString<SummaryDto>(contentJson)
            
            Result.Success(Summary(
                mainIdea = dto.mainIdea,
                keyPoints = dto.keyPoints,
                sentiment = dto.sentiment,
                clickbaitScore = dto.clickbaitScore,
                tags = dto.tags,
                title = content.title,
                originalUrl = content.url,
                wasTruncated = content.wasTruncated
            ))

        } catch (e: Exception) {
            logger.error(e) { "OpenAI request failed" }
            Result.Failure(SummarizerError.AIError(e.message ?: "Unknown AI error"))
        }
    }

    private companion object {
        const val MODEL = "gpt-4o-mini"
        val SYSTEM_PROMPT = """
            Ты — Senior AI Analyst. Твоя цель — структурировать входящую информацию для занятых профессионалов.
            
            ТРЕБОВАНИЯ К АНАЛИЗУ:
            1. Main Idea: Должна отвечать на вопрос "Почему это важно?".
            2. Key Points: Выдели основные факты, цифры или аргументы (от 3 до 10 пунктов). Обязательно отрази все ключевые разделы, упомянутые в статье. Избегай общих фраз.
            3. Sentiment: Определи тон (Positive, Neutral, Negative, Technical).
            4. Clickbait Score: Оцени от 0 до 10, насколько заголовок или контент манипулятивны.
            5. Tags: 3-5 ключевых тегов (хештегов) на русском.
            
            ЯЗЫК: Русский (Russian).
            ФОРМАТ: Строгий JSON без комментариев.
            
            {
                "mainIdea": "string",
                "keyPoints": ["string", "string"],
                "sentiment": "string",
                "clickbaitScore": int,
                "tags": ["string", "string"]
            }
        """.trimIndent()
    }
}

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat,
    val temperature: Double = 0.7
)

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ResponseFormat(val type: String)

@Serializable
private data class ChatCompletionResponse(val choices: List<Choice>)

@Serializable
private data class Choice(val message: ChatMessage)

@Serializable
private data class SummaryDto(
    val mainIdea: String,
    val keyPoints: List<String>,
    val sentiment: String,
    val clickbaitScore: Int,
    val tags: List<String> = emptyList()
)
