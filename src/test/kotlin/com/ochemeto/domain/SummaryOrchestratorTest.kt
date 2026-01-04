package com.ochemeto.domain

import com.ochemeto.infrastructure.AIProvider
import com.ochemeto.infrastructure.ContentExtractor
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class SummaryOrchestratorTest : StringSpec({

    val extractor = mockk<ContentExtractor>()
    val aiProvider = mockk<AIProvider>()
    val orchestrator = SummaryOrchestratorImpl(extractor, aiProvider)

    "processSummaryRequest should return success when both services succeed" {
        val url = "http://test.com"
        val content = ExtractedContent("text", "title", url)
        val summary = Summary(
            mainIdea = "Idea",
            keyPoints = listOf("P1"),
            sentiment = "Neutral",
            clickbaitScore = 0,
            tags = listOf("tag"),
            title = "title",
            originalUrl = url
        )

        coEvery { extractor.extractContent(url) } returns Result.Success(content)
        coEvery { aiProvider.generateSummary(content) } returns Result.Success(summary)

        val result = orchestrator.processSummaryRequest(url)

        result shouldBe Result.Success(summary)
    }

    "processSummaryRequest should return failure when extractor fails" {
        val url = "http://test.com"
        val error = SummarizerError.NetworkError("404")

        coEvery { extractor.extractContent(url) } returns Result.Failure(error)

        val result = orchestrator.processSummaryRequest(url)

        (result as Result.Failure).error shouldBe error
    }

    "processSummaryRequest should return failure when AI fails" {
        val url = "http://test.com"
        val content = ExtractedContent("text", "title", url)
        val error = SummarizerError.AIError("Timeout")

        coEvery { extractor.extractContent(url) } returns Result.Success(content)
        coEvery { aiProvider.generateSummary(content) } returns Result.Failure(error)

        val result = orchestrator.processSummaryRequest(url)

        (result as Result.Failure).error shouldBe error
    }
})
