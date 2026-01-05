package com.ochemeto.domain

import com.ochemeto.infrastructure.AIProvider
import com.ochemeto.infrastructure.ContentExtractor
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

interface SummaryOrchestrator {
    suspend fun processSummaryRequest(url: String): Result<Summary>
    suspend fun summarizeText(text: String, wasTruncated: Boolean = false): Summary
}

class SummaryOrchestratorImpl(
    private val contentExtractor: ContentExtractor,
    private val aiProvider: AIProvider
) : SummaryOrchestrator {

    override suspend fun processSummaryRequest(url: String): Result<Summary> {
        logger.info { "Processing: $url" }
        
        return try {
            val contentResult = contentExtractor.extractContent(url)
            
            val content = when (contentResult) {
                is Result.Success -> contentResult.value
                is Result.Failure -> {
                    logger.warn { "Extraction failed ($url): ${contentResult.error}" }
                    return Result.Failure(contentResult.error)
                }
            }

            val summaryResult = aiProvider.generateSummary(content)
            
            if (summaryResult is Result.Failure) {
                logger.error { "AI failed ($url): ${summaryResult.error}" }
            }
            
            summaryResult

        } catch (e: Exception) {
            logger.error(e) { "Orchestration error ($url)" }
            Result.Failure(SummarizerError.UnknownError(e))
        }
    }

    override suspend fun summarizeText(text: String, wasTruncated: Boolean): Summary {
        logger.info { "Summarizing text (length: ${text.length}, wasTruncated: $wasTruncated)" }
        
        val content = ExtractedContent(
            text = text,
            title = null,
            url = "voice_message",
            wasTruncated = wasTruncated
        )
        
        val summaryResult = aiProvider.generateSummary(content)
        
        return when (summaryResult) {
            is Result.Success -> summaryResult.value
            is Result.Failure -> {
                logger.error { "AI failed to summarize text: ${summaryResult.error}" }
                throw RuntimeException("Failed to summarize text: ${summaryResult.error}")
            }
        }
    }
}
