package com.ochemeto.domain.voice

import com.ochemeto.infrastructure.voice.FileDownloader
import com.ochemeto.infrastructure.voice.TranscriptionProvider
import com.ochemeto.domain.SummaryOrchestrator
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private const val MAX_TEXT_LENGTH = 50000

interface VoiceTranscriptionOrchestrator {
    suspend fun processVoice(fileId: String): VoiceProcessingResult
}

class VoiceTranscriptionOrchestratorImpl(
    private val fileDownloader: FileDownloader,
    private val transcriptionProvider: TranscriptionProvider,
    private val summaryOrchestrator: SummaryOrchestrator,
    private val shortTextThreshold: Int = 50
) : VoiceTranscriptionOrchestrator {

    override suspend fun processVoice(fileId: String): VoiceProcessingResult {
        val audioBytes = fileDownloader.downloadFile(fileId)
        val transcribedText = transcriptionProvider.transcribe(audioBytes)

        return if (transcribedText.length < shortTextThreshold) {
            VoiceProcessingResult.ShortMessage(transcribedText)
        } else {
            val wasTruncated = transcribedText.length > MAX_TEXT_LENGTH
            val textForSummary = if (wasTruncated) {
                logger.warn { "Text truncated: ${transcribedText.length} -> $MAX_TEXT_LENGTH" }
                transcribedText.substring(0, MAX_TEXT_LENGTH)
            } else {
                transcribedText
            }

            val summary = summaryOrchestrator.summarizeText(textForSummary, wasTruncated)
            VoiceProcessingResult.LongMessageWithSummary(transcribedText, summary)
        }
    }
}
