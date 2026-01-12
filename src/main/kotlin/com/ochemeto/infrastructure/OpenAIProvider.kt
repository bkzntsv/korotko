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
        // Определяем тип контента: голосовое сообщение или статья
        val isVoiceMessage = content.url == "voice_message"
        val systemPrompt = if (isVoiceMessage) VOICE_MESSAGE_PROMPT else SYSTEM_PROMPT
        
        val request = ChatCompletionRequest(
            model = MODEL,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", content.text)
            ),
            responseFormat = ResponseFormat("json_object")
        )

        return try {
            val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Проверяем статус ответа перед десериализацией
            if (response.status.value !in 200..299) {
                val errorBody = try {
                    response.body<String>()
                } catch (e: Exception) {
                    "Unable to read error body: ${e.message}"
                }
                logger.error { "OpenAI API error: ${response.status} - $errorBody" }
                
                // Формируем понятное сообщение для пользователя
                val userMessage = when (response.status.value) {
                    429 -> "Превышен лимит запросов к OpenAI. Попробуйте позже."
                    401 -> "Ошибка аутентификации OpenAI API. Проверьте API ключ."
                    500, 502, 503, 504 -> "Временная ошибка сервера OpenAI. Попробуйте позже."
                    else -> "Ошибка OpenAI API: ${response.status}"
                }
                return Result.Failure(SummarizerError.AIError(userMessage))
            }

            // Пытаемся десериализовать успешный ответ
            val chatResponse = try {
                response.body<ChatCompletionResponse>()
            } catch (e: kotlinx.serialization.SerializationException) {
                // Если десериализация не удалась, возможно это ошибка API в формате JSON
                // Но body уже прочитан, поэтому нужно использовать другой подход
                logger.error { "Failed to deserialize OpenAI response: $e" }
                return Result.Failure(SummarizerError.AIError("Invalid response format from OpenAI: ${e.message}"))
            }

            val contentJson = chatResponse.choices.firstOrNull()?.message?.content
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
        const val MODEL = "gpt-5-nano"
        
        val SYSTEM_PROMPT = """
           You are an expert in information synthesis and data distillation. Your goal is to transform long-form text into a highly readable, concise summary for a busy audience.

CONTENT REQUIREMENTS:
1. mainIdea: A single short sentence. Explain the essence and why it matters.
2. keyPoints: 
   - If the text contains a specific list (e.g., "10 tips"), list every single item.
   - If it is a standard longread, extract the 5-7 most significant insights.
   - Use ONLY simple, short sentences. No subordinate clauses or complex structures.
   - Strictly avoid long dashes (—). Use a regular hyphen (-) or no dash at all.
   - Avoid meta-references like "The article states," "The author mentions," or "The text describes." State facts directly.
3. sentiment: One word (Positive, Neutral, or Critical).
4. clickbaitScore: An integer from 0 to 10, assessing how much the headline manipulates the reader.

OUTPUT STYLE:
- Professional, dry, and factual. 
- Zero "filler" words.
- High information density.

LANGUAGE:
- The values in the JSON must be in Russian.

FORMAT:
- Return ONLY strict JSON. No markdown blocks, no preamble.

JSON STRUCTURE:
{
    "mainIdea": "string",
    "keyPoints": ["string"],
    "sentiment": "string",
    "clickbaitScore": int
}
        """.trimIndent()
        
        val VOICE_MESSAGE_PROMPT = """
            Ты — AI-ассистент, который анализирует голосовые сообщения. Твоя задача — извлечь суть из разговорной речи, учитывая особенности устной коммуникации.
            
            ОСОБЕННОСТИ ГОЛОСОВЫХ СООБЩЕНИЙ:
            - Могут содержать паузы, повторы, междометия ("ээ", "ну", "вот")
            - Разговорная речь, неформальный стиль
            - Могут быть вопросы, просьбы, мнения, инструкции
            - Контекст может быть неполным или имплицитным
            - Важно уловить намерение говорящего, а не только факты
            
            ТРЕБОВАНИЯ К АНАЛИЗУ:
            1. Main Idea: Сформулируй главную мысль или намерение говорящего. Если это вопрос — укажи, о чем спрашивают. Если просьба — что просят. Если мнение — в чем суть позиции. Если информация — ключевой вывод.
            2. Key Points: Выдели основные моменты (от 3 до 8 пунктов):
               - Конкретные факты, цифры, даты, имена
               - Важные детали или примеры
               - Ключевые аргументы или рассуждения
               - Действия или планы, если упомянуты
               - Игнорируй повторы и междометия, но сохраняй смысл
            3. Sentiment: Определи эмоциональный тон:
               - Positive — позитивный, воодушевленный, довольный
               - Neutral — нейтральный, информативный, деловой
               - Negative — негативный, обеспокоенный, критичный
               - Question — вопрос, запрос информации
               - Request — просьба, инструкция
            4. Clickbait Score: Для голосовых сообщений всегда ставь 0 (не применимо).
            5. Tags: 3-5 ключевых тегов на русском, отражающих темы сообщения.
            
            ВАЖНО:
            - Не добавляй информацию, которой нет в тексте
            - Сохраняй оригинальный смысл, даже если речь неформальная
            - Если текст содержит вопросы — отрази их в keyPoints
            - Если есть неопределенность — укажи это в mainIdea
            
            ЯЗЫК: Русский (Russian).
            ФОРМАТ: Строгий JSON без комментариев.
            
            {
                "mainIdea": "string",
                "keyPoints": ["string", "string"],
                "sentiment": "string",
                "clickbaitScore": 0,
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
