package com.ochemeto.infrastructure.voice

import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.ochemeto.domain.voice.TranscriptionException
import okio.buffer
import okio.source
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class OpenAiTranscriptionProvider(
    private val openAiClient: OpenAI,
    private val model: String = "whisper-1",
    private val language: String = "ru"
) : TranscriptionProvider {

    override suspend fun transcribe(audioBytes: ByteArray): String {
        return try {
            val fileSource = FileSource(
                name = "voice.ogg",
                source = audioBytes.inputStream().source().buffer()
            )

            val request = com.aallam.openai.api.audio.TranscriptionRequest(
                audio = fileSource,
                model = ModelId(model),
                language = language
            )
            
            val transcription = openAiClient.transcription(request)
            transcription.text
            
        } catch (e: Exception) {
            logger.error(e) { "Transcription failed" }
            throw TranscriptionException("Transcription failed: ${e.message}", e)
        }
    }
}
