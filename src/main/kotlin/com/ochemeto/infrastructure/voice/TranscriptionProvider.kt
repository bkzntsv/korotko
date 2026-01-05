package com.ochemeto.infrastructure.voice

interface TranscriptionProvider {
    suspend fun transcribe(audioBytes: ByteArray): String
}
