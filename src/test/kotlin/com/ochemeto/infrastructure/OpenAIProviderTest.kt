package com.ochemeto.infrastructure

import com.ochemeto.domain.ExtractedContent
import com.ochemeto.domain.Result
import com.ochemeto.domain.SummarizerError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class OpenAIProviderTest : StringSpec({

    val json = Json { ignoreUnknownKeys = true }

    "generateSummary should correctly parse successful JSON response" {
        val mockEngine = MockEngine { request ->
            val body = (request.body as TextContent).text
            body shouldContain "gpt-4o-mini"
            body shouldContain "json_object"
            // Проверяем наличие новых полей в промпте (косвенно, через system prompt)
            body shouldContain "Senior AI Analyst" 

            respond(
                content = """
                    {
                        "choices": [
                            {
                                "message": {
                                    "role": "assistant",
                                    "content": "{\"mainIdea\": \"Test Idea\", \"keyPoints\": [\"Point 1\"], \"sentiment\": \"Positive\", \"clickbaitScore\": 2, \"tags\": [\"AI\"]}"
                                }
                            }
                        ]
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        
        val provider = OpenAIProvider("key", client)
        val content = ExtractedContent("Some text", "Title", "http://url.com")
        
        val result = provider.generateSummary(content)
        
        result.isSuccess() shouldBe true
        val summary = result.getOrNull()!!
        summary.mainIdea shouldBe "Test Idea"
        summary.sentiment shouldBe "Positive"
        summary.clickbaitScore shouldBe 2
        summary.tags shouldBe listOf("AI")
    }

    "generateSummary should handle AI error" {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError)
        }
        
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        
        val provider = OpenAIProvider("key", client)
        val content = ExtractedContent("Some text", "Title", "http://url.com")
        
        val result = provider.generateSummary(content)
        
        result.isFailure() shouldBe true
    }
})
