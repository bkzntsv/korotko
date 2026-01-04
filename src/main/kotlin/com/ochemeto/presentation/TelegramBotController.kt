package com.ochemeto.presentation

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.ParseMode
import com.ochemeto.config.BotConfig
import com.ochemeto.domain.Result
import com.ochemeto.domain.SummaryOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class TelegramBotController(
    private val config: BotConfig,
    private val orchestrator: SummaryOrchestrator
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        val bot = bot {
            token = config.telegramToken
            
            dispatch {
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
                    scope.launch { handleMessage(bot, message) }
                }
            }
        }
        
        logger.info { "Bot started" }
        bot.startPolling()
    }

    private suspend fun handleMessage(bot: com.github.kotlintelegrambot.Bot, message: Message) {
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

        bot.sendMessage(
            chatId = chatId,
            text = responseText,
            parseMode = ParseMode.MARKDOWN,
            disableWebPagePreview = true
        )
    }

    private fun extractUrl(message: Message): String? {
        val text = message.text ?: return null
        
        // Priority: Telegram Entities -> Regex
        message.entities?.firstOrNull { it.type == MessageEntity.Type.URL }?.let { entity ->
            return text.substring(entity.offset, entity.offset + entity.length)
        }
        
        message.entities?.firstOrNull { it.type == MessageEntity.Type.TEXT_LINK }?.let { entity ->
            return entity.url
        }

        return "(https?://\\S+)".toRegex().find(text)?.value
    }
}
