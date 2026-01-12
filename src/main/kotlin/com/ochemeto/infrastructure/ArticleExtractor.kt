package com.ochemeto.infrastructure

import com.ochemeto.config.BotConfig
import com.ochemeto.domain.ExtractedContent
import com.ochemeto.domain.Result
import com.ochemeto.domain.SummarizerError
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
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
        // Use ZenRows for LinkedIn or if explicitly configured
        val useZenRows = config.zenRowsApiKey != null && 
            (url.contains("linkedin.com", ignoreCase = true) || url.contains("lnkd.in", ignoreCase = true))

        if (useZenRows) {
            return fetchWithZenRows(url, config.zenRowsApiKey!!)
        }

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
                HttpStatusCode.Forbidden -> {
                     if (config.zenRowsApiKey != null) {
                         logger.warn { "Access denied (403), retrying with ZenRows" }
                         fetchWithZenRows(url, config.zenRowsApiKey!!)
                     } else {
                         Result.Failure(SummarizerError.NetworkError("Access denied (403)"))
                     }
                }
                HttpStatusCode.NotFound -> Result.Failure(SummarizerError.NetworkError("Page not found (404)"))
                else -> {
                    if (response.status.value == 999 && config.zenRowsApiKey != null) {
                        logger.warn { "Server error (999), retrying with ZenRows" }
                        fetchWithZenRows(url, config.zenRowsApiKey!!)
                    } else if (response.status.value >= 500) {
                        Result.Failure(SummarizerError.NetworkError("Server error (${response.status.value})"))
                    } else {
                        // Try to parse anyway for other 2xx codes
                        processResponse(response.bodyAsText(), url)
                    }
                }
            }
        } catch (e: ConnectTimeoutException) {
            logger.warn { "Connection timeout for $url" }
            Result.Failure(SummarizerError.NetworkError("Connection timeout. Site may be slow or blocking requests."))
        } catch (e: HttpRequestTimeoutException) {
            logger.warn { "Request timeout for $url" }
            Result.Failure(SummarizerError.NetworkError("Request timeout. Site took too long to respond."))
        } catch (e: Exception) {
            logger.error(e) { "Extraction failed for $url" }
            Result.Failure(SummarizerError.UnknownError(e))
        }
    }

    private suspend fun fetchWithZenRows(originalUrl: String, apiKey: String): Result<ExtractedContent> {
        return try {
            logger.info { "Fetching via ZenRows: $originalUrl" }
            val encodedUrl = originalUrl.encodeURLParameter()
            
            // Пробуем минимальную конфигурацию, которая часто работает стабильнее
            // js_render=true обязателен для LinkedIn (SPA)
            // premium_proxy=true обязателен для обхода 999 и маскировки
            val zenRowsUrl = "https://api.zenrows.com/v1/?apikey=$apiKey&url=$encodedUrl&js_render=true&premium_proxy=true"
            
            val response = httpClient.get(zenRowsUrl)
            
            if (response.status == HttpStatusCode.OK) {
                processResponse(response.bodyAsText(), originalUrl)
            } else {
                 Result.Failure(SummarizerError.NetworkError("ZenRows error (${response.status.value}): ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            logger.error(e) { "ZenRows extraction failed for $originalUrl" }
            Result.Failure(SummarizerError.UnknownError(e))
        }
    }

    private fun processResponse(html: String, url: String): Result<ExtractedContent> {
        val doc = Jsoup.parse(html, url)
        
        // Для LinkedIn не используем Readability, так как он вырезает списки опыта работы
        val isLinkedIn = url.contains("linkedin.com", ignoreCase = true) || url.contains("lnkd.in", ignoreCase = true)
        
        var text: String
        var title: String? = null
        
        if (isLinkedIn) {
            // Специфичная логика для LinkedIn: просто берем весь текст body, так как Readability ломает профили
            // Удаляем явный мусор
            doc.select("script, style, nav, footer, .ad, iframe").remove()
            text = doc.body().text()
            title = doc.title()
        } else {
            // Стандартная логика для статей
            val readability = Readability4J(url, doc).parse()
            text = readability.textContent?.takeIf { it.isNotBlank() } ?: manualExtraction(doc)
            title = readability.title ?: doc.title()
        }

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
            title = title,
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
