package com.ochemeto.presentation

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.audio
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.dispatcher.voice
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.ParseMode
import com.ochemeto.domain.Result
import com.ochemeto.domain.SummaryOrchestrator
import com.ochemeto.domain.voice.DownloadException
import com.ochemeto.domain.voice.FileSizeLimitExceededException
import com.ochemeto.domain.voice.TranscriptionException
import com.ochemeto.domain.voice.VoiceProcessingResult
import com.ochemeto.domain.voice.VoiceTranscriptionOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val scopeJob = SupervisorJob()
private val scope = CoroutineScope(scopeJob + Dispatchers.IO)

fun cancelBotControllerScope() {
    scopeJob.cancel()
}

private const val TELEGRAM_MAX_MESSAGE_LENGTH = 4096
private const val CONTINUATION_PREFIX = "_(продолжение)_\n"

fun Dispatcher.setupTextHandlers(orchestrator: SummaryOrchestrator) {
    command("start") {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "Привет! Отправь мне ссылку на статью, и я сделаю краткую выжимку. Я понимаю статьи на любом языке, но отвечаю всегда по-русски"
        )
    }

    command("help") {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "📖 **Инструкция:**\nОтправь ссылку (например, https://habr.com/...). Я пришлю главную идею и тезисы.",
            parseMode = ParseMode.MARKDOWN
        )
    }

    text {
        if (message.text?.startsWith("/") == true) return@text
        scope.launch { handleMessage(bot, message, orchestrator) }
    }
}

fun Dispatcher.setupVoiceHandlers(orchestratorProvider: () -> VoiceTranscriptionOrchestrator) {
    voice {
        val chatId = ChatId.fromId(message.chat.id)
        val fileId = message.voice?.fileId ?: return@voice
        scope.launch {
            handleVoiceMessage(bot, chatId, fileId, orchestratorProvider())
        }
    }
    
    audio {
        val chatId = ChatId.fromId(message.chat.id)
        val fileId = message.audio?.fileId ?: return@audio
        scope.launch {
            handleVoiceMessage(bot, chatId, fileId, orchestratorProvider())
        }
    }
}

private suspend fun handleMessage(bot: Bot, message: Message, orchestrator: SummaryOrchestrator) {
    val chatId = ChatId.fromId(message.chat.id)
    
    val url = extractUrl(message)
    if (url == null) {
        bot.sendMessage(chatId, "⚠️ Ссылка не найдена.")
        return
    }

    bot.sendChatAction(chatId, ChatAction.TYPING)

    val responseText = when (val result = orchestrator.processSummaryRequest(url)) {
        is Result.Success -> ResponseFormatter.formatSummary(result.value, result.value.wasTruncated)
        is Result.Failure -> ResponseFormatter.formatError(result.error)
    }

    sendChunkedMessage(bot, chatId, responseText)
}

private fun extractUrl(message: Message): String? {
    val text = message.text ?: return null
    
    message.entities?.firstOrNull { it.type == MessageEntity.Type.URL }?.let { entity ->
        return text.substring(entity.offset, entity.offset + entity.length)
    }
    
    message.entities?.firstOrNull { it.type == MessageEntity.Type.TEXT_LINK }?.let { entity ->
        return entity.url
    }

    return "(https?://\\S+)".toRegex().find(text)?.value
}

private suspend fun sendChunkedMessage(bot: Bot, chatId: ChatId, text: String) {
    if (text.length <= TELEGRAM_MAX_MESSAGE_LENGTH) {
        bot.sendMessage(
            chatId = chatId,
            text = text,
            parseMode = ParseMode.MARKDOWN,
            disableWebPagePreview = true
        )
        return
    }
    
    val chunkSize = TELEGRAM_MAX_MESSAGE_LENGTH - CONTINUATION_PREFIX.length
    val chunks = text.chunked(chunkSize)
    
    chunks.forEachIndexed { index, chunk ->
        val messageText = if (index == 0) chunk else "$CONTINUATION_PREFIX$chunk"
        bot.sendMessage(
            chatId = chatId,
            text = messageText,
            parseMode = ParseMode.MARKDOWN,
            disableWebPagePreview = true
        )
    }
}

private suspend fun handleVoiceMessage(
    bot: Bot,
    chatId: ChatId,
    fileId: String,
    orchestrator: VoiceTranscriptionOrchestrator
) {
    try {
        bot.sendChatAction(chatId, ChatAction.TYPING)
        bot.sendMessage(chatId, "🎧 Слушаю...")
        
        val result = orchestrator.processVoice(fileId)
        
        when (result) {
            is VoiceProcessingResult.ShortMessage -> {
                bot.sendMessage(
                    chatId,
                    "🗣 *Текст:* ${result.text}",
                    parseMode = ParseMode.MARKDOWN
                )
            }
            is VoiceProcessingResult.LongMessageWithSummary -> {
                bot.sendMessage(chatId, "📄 Текст распознан. Анализирую...")
                val summaryText = ResponseFormatter.formatSummary(result.summary, result.summary.wasTruncated, isVoiceMessage = true)
                sendChunkedMessage(bot, chatId, summaryText)
            }
        }
    } catch (e: Exception) {
        logger.error(e) { "Voice message processing failed" }
        val userMessage = when (e) {
            is FileSizeLimitExceededException -> "❌ Файл слишком большой (лимит 20 МБ)."
            is DownloadException -> "❌ Не удалось скачать файл с серверов Telegram."
            is TranscriptionException -> "❌ OpenAI не смог распознать речь."
            else -> "❌ Произошла ошибка. Попробуйте позже."
        }
        bot.sendMessage(chatId, userMessage)
    }
}
