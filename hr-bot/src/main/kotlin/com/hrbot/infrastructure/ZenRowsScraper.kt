package com.hrbot.infrastructure

import com.hrbot.domain.ContentProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import org.jsoup.Jsoup
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ZenRowsScraper(
    private val apiKey: String,
    private val httpClient: HttpClient
) : ContentProvider {

    override suspend fun getContent(url: String): String {
        logger.info { "Scraping URL with ZenRows: $url" }
        
        try {
            val response = httpClient.get("https://api.zenrows.com/v1/") {
                parameter("apikey", apiKey)
                parameter("url", url)
                parameter("js_render", "true")
                parameter("antibot", "true")
                parameter("premium_proxy", "true")
            }
            
            val html = response.bodyAsText()
            
            // Clean up HTML
            val doc = Jsoup.parse(html)
            
            // Remove scripts and styles
            doc.select("script, style, nav, footer, iframe, header").remove()
            
            // Get text
            val text = doc.text()
            
            // Basic cleanup
            return text.replace("\\s+".toRegex(), " ").trim()
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to scrape url: $url" }
            throw e
        }
    }
}
