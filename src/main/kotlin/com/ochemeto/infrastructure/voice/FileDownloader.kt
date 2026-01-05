package com.ochemeto.infrastructure.voice

interface FileDownloader {
    suspend fun downloadFile(fileId: String): ByteArray
}
