package com.ochemeto.infrastructure

import com.ochemeto.config.BotConfig
import com.ochemeto.domain.ExtractedContent
import com.ochemeto.domain.Result
import com.ochemeto.domain.SummarizerError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

interface ContentExtractor {
    suspend fun extractContent(url: String): Result<ExtractedContent>
}

class ArticleExtractor(
    private val httpClient: HttpClient,
    private val config: BotConfig
) : ContentExtractor {

    private companion object {
        const val MIN_CONTENT_LENGTH = 200
        const val MAX_CONTENT_LENGTH = 50000
        const val REMOVE_SELECTORS = "nav, footer, aside, script, style, .ad, .advertisement, .cookie-banner"
        const val CONTENT_SELECTORS = "article, main, .content, .post-content, #content"
        const val TEXT_SELECTORS = "p, h1, h2, h3, h4"
    }

    override suspend fun extractContent(url: String): Result<ExtractedContent> {
        return try {
            logger.info { "Fetching URL: $url" }
            
            val response = httpClient.get(url) {
                header(HttpHeaders.UserAgent, config.userAgent)
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                header(HttpHeaders.AcceptLanguage, "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                header("Sec-Ch-Ua-Mobile", "?0")
                header("Sec-Fetch-Dest", "document")
                header("Sec-Fetch-Mode", "navigate")
                header("Sec-Fetch-Site", "none")
                header("Upgrade-Insecure-Requests", "1")
            }

            when (response.status) {
                HttpStatusCode.OK -> processResponse(response.bodyAsText(), url)
                HttpStatusCode.Forbidden -> Result.Failure(SummarizerError.NetworkError("Access denied (403)"))
                HttpStatusCode.NotFound -> Result.Failure(SummarizerError.NetworkError("Page not found (404)"))
                else -> {
                    if (response.status.value >= 500) {
                        Result.Failure(SummarizerError.NetworkError("Server error (${response.status.value})"))
                    } else {
                        // Try to parse anyway for other 2xx codes
                        processResponse(response.bodyAsText(), url)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Extraction failed for $url" }
            Result.Failure(SummarizerError.UnknownError(e))
        }
    }

    private fun processResponse(html: String, url: String): Result<ExtractedContent> {
        val doc = Jsoup.parse(html, url)
        val readability = Readability4J(url, doc).parse()
        
        var text = readability.textContent?.takeIf { it.isNotBlank() } 
            ?: manualExtraction(doc)

        text = text.trim()

        if (text.length < MIN_CONTENT_LENGTH) {
            return Result.Failure(SummarizerError.ParsingError("Content too short or protected (${text.length} chars)"))
        }

        val (finalText, truncated) = if (text.length > MAX_CONTENT_LENGTH) {
            text.substring(0, MAX_CONTENT_LENGTH) to true
        } else {
            text to false
        }

        return Result.Success(ExtractedContent(
            text = finalText,
            title = readability.title ?: doc.title(),
            url = url,
            wasTruncated = truncated
        ))
    }

    private fun manualExtraction(doc: org.jsoup.nodes.Document): String {
        doc.select(REMOVE_SELECTORS).remove()
        val mainContent = doc.select(CONTENT_SELECTORS).firstOrNull() ?: doc.body()
        return mainContent.select(TEXT_SELECTORS).joinToString("\n\n") { it.text() }
    }
}
