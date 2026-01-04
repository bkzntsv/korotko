package com.ochemeto.infrastructure

import com.ochemeto.config.BotConfig
import com.ochemeto.domain.SummarizerError
import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

class ArticleExtractorTest : StringSpec({

    val config = BotConfig("token", "key")

    "extractContent should return NetworkError on 403 Forbidden" {
        val mockEngine = MockEngine { 
            respond("", HttpStatusCode.Forbidden)
        }
        val client = HttpClient(mockEngine)
        val extractor = ArticleExtractor(client, config)

        val result = extractor.extractContent("http://example.com")
        
        result.isFailure() shouldBe true
        (result as com.ochemeto.domain.Result.Failure).error shouldBe SummarizerError.NetworkError("Access denied (403)")
    }

    "extractContent should return NetworkError on 404 Not Found" {
        val mockEngine = MockEngine { 
            respond("", HttpStatusCode.NotFound)
        }
        val client = HttpClient(mockEngine)
        val extractor = ArticleExtractor(client, config)

        val result = extractor.extractContent("http://example.com")
        
        (result as com.ochemeto.domain.Result.Failure).error shouldBe SummarizerError.NetworkError("Page not found (404)")
    }

    // Property Test 12: Content Length Validation
    "extractContent should return ParsingError for content < 200 chars" {
        // Generate small content
        val smallTexts = listOf(
            "Short text",
            "A".repeat(199)
        ).exhaustive()

        checkAll(smallTexts) { text ->
            val html = "<html><body><p>$text</p></body></html>"
            val mockEngine = MockEngine {
                respond(html, headers = headersOf(HttpHeaders.ContentType, "text/html"))
            }
            val client = HttpClient(mockEngine)
            val extractor = ArticleExtractor(client, config)

            val result = extractor.extractContent("http://example.com")
            
            result.isFailure() shouldBe true
            val error = (result as com.ochemeto.domain.Result.Failure).error
            // Dynamic message based on length
            error shouldBe SummarizerError.ParsingError("Content too short or protected (${text.length} chars)")
        }
    }

    // Property Test 13: Content Truncation
    "extractContent should truncate content > 50000 chars" {
        val longText = "A".repeat(50001)
        val html = "<html><body><p>$longText</p></body></html>"
        
        val mockEngine = MockEngine {
            respond(html, headers = headersOf(HttpHeaders.ContentType, "text/html"))
        }
        val client = HttpClient(mockEngine)
        val extractor = ArticleExtractor(client, config)

        val result = extractor.extractContent("http://example.com")
        
        result.isSuccess() shouldBe true
        val content = result.getOrNull()!!
        content.text.length shouldBe 50000
        content.wasTruncated.shouldBeTrue()
    }

    // Property Test 4: Content Structure Preservation (Basic)
    "extractContent should preserve basic structure and title" {
        val title = "Test Article Title"
        val p1 = "This is paragraph one."
        val p2 = "This is paragraph two."
        // Add padding to pass length validation
        val padding = "Content padding ".repeat(20) 
        
        val html = """
            <html>
                <head><title>$title</title></head>
                <body>
                    <article>
                        <h1>Header 1</h1>
                        <p>$p1</p>
                        <p>$p2</p>
                        <p>$padding</p>
                    </article>
                </body>
            </html>
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(html, headers = headersOf(HttpHeaders.ContentType, "text/html"))
        }
        val client = HttpClient(mockEngine)
        val extractor = ArticleExtractor(client, config)

        val result = extractor.extractContent("http://example.com")
        
        result.isSuccess() shouldBe true
        val content = result.getOrNull()!!
        
        content.title shouldBe title
        content.text shouldContain p1
        content.text shouldContain p2
        content.wasTruncated.shouldBeFalse()
    }
    
    // Property Test 10: User-Agent
    "extractContent should use configured User-Agent" {
        val mockEngine = MockEngine { request ->
            request.headers[HttpHeaders.UserAgent] shouldBe config.userAgent
            respond("<html><body><p>${"A".repeat(300)}</p></body></html>")
        }
        val client = HttpClient(mockEngine)
        val extractor = ArticleExtractor(client, config)
        
        extractor.extractContent("http://example.com")
    }
})

