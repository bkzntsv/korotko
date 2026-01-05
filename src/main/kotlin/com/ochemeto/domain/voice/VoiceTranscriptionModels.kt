package com.ochemeto.domain.voice

import com.ochemeto.domain.Summary

sealed class VoiceProcessingResult {
    data class ShortMessage(val text: String) : VoiceProcessingResult()
    data class LongMessageWithSummary(
        val rawText: String,
        val summary: Summary
    ) : VoiceProcessingResult()
}

sealed class VoiceTranscriptionException(message: String, cause: Throwable? = null) 
    : Exception(message, cause)

class DownloadException(message: String, cause: Throwable? = null) 
    : VoiceTranscriptionException(message, cause)

class TranscriptionException(message: String, cause: Throwable? = null) 
    : VoiceTranscriptionException(message, cause)

class FileSizeLimitExceededException(message: String, cause: Throwable? = null) 
    : VoiceTranscriptionException(message, cause)

data class VoiceTranscriptionConfig(
    val shortTextThreshold: Int = 50,
    val maxFileSizeMb: Int = 20,
    val whisperModel: String = "whisper-1",
    val defaultLanguage: String = "ru"
)
