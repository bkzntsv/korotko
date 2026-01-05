package com.ochemeto.domain.voice

import com.ochemeto.domain.Summary
import com.ochemeto.domain.SummaryOrchestrator
import com.ochemeto.infrastructure.voice.FileDownloader
import com.ochemeto.infrastructure.voice.TranscriptionProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk

class VoiceTranscriptionOrchestratorTest : StringSpec({

    val downloader = mockk<FileDownloader>()
    val transcriber = mockk<TranscriptionProvider>()
    val summaryOrchestrator = mockk<SummaryOrchestrator>()
    
    val orchestrator = VoiceTranscriptionOrchestratorImpl(
        fileDownloader = downloader,
        transcriptionProvider = transcriber,
        summaryOrchestrator = summaryOrchestrator,
        shortTextThreshold = 50
    )

    "processVoice should return ShortMessage when text is short" {
        val fileId = "file123"
        val bytes = byteArrayOf(1, 2, 3)
        val shortText = "Short text" // < 50 chars

        coEvery { downloader.downloadFile(fileId) } returns bytes
        coEvery { transcriber.transcribe(bytes) } returns shortText

        val result = orchestrator.processVoice(fileId)

        result.shouldBeInstanceOf<VoiceProcessingResult.ShortMessage>()
    }

    "processVoice should return LongMessageWithSummary when text is long" {
        val fileId = "file456"
        val bytes = byteArrayOf(4, 5, 6)
        val longText = "This is a very long text that definitely exceeds the fifty character threshold for summarization." // > 50 chars
        val summary = Summary(
            mainIdea = "Main idea",
            keyPoints = listOf("Point 1"),
            sentiment = "Neutral",
            clickbaitScore = 0,
            tags = listOf("tag"),
            title = null,
            originalUrl = "voice_message"
        )

        coEvery { downloader.downloadFile(fileId) } returns bytes
        coEvery { transcriber.transcribe(bytes) } returns longText
        coEvery { summaryOrchestrator.summarizeText(longText, false) } returns summary

        val result = orchestrator.processVoice(fileId)

        result.shouldBeInstanceOf<VoiceProcessingResult.LongMessageWithSummary>()
    }
})

