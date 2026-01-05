package com.ochemeto.infrastructure.voice

import com.github.kotlintelegrambot.Bot
import com.ochemeto.domain.voice.DownloadException
import com.ochemeto.domain.voice.FileSizeLimitExceededException
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private const val MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L

class TelegramFileDownloader(
    private val bot: Bot,
    private val token: String,
    private val httpClient: HttpClient
) : FileDownloader {

    override suspend fun downloadFile(fileId: String): ByteArray {
        val responsePair = bot.getFile(fileId)
        val response = responsePair.first
        val exception = responsePair.second
        
        if (exception != null) {
            throw DownloadException("Telegram API error: ${exception.message}", exception)
        }

        val fileData = response?.body()?.result
            ?: throw DownloadException("Failed to get file data")

        val fileSize = fileData.fileSize?.toLong() ?: 0L
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw FileSizeLimitExceededException("File size exceeds limit: $fileSize bytes")
        }

        val filePath = fileData.filePath
            ?: throw DownloadException("File path is empty")

        val downloadUrl = "https://api.telegram.org/file/bot$token/$filePath"

        return try {
            val httpResponse: HttpResponse = httpClient.get(downloadUrl)
            if (httpResponse.status.value in 200..299) {
                httpResponse.readBytes()
            } else {
                throw DownloadException("Telegram server returned error: ${httpResponse.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to download file: $downloadUrl" }
            throw DownloadException("File download failed: ${e.message}", e)
        }
    }
}
