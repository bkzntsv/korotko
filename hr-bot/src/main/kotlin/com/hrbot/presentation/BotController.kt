package com.hrbot.presentation

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.logging.LogLevel
import com.hrbot.config.Config
import com.hrbot.domain.RecruitmentOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BotController(
    private val orchestrator: RecruitmentOrchestrator
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        logger.info { "Initializing bot with token: ${Config.telegramToken.take(4)}...${Config.telegramToken.takeLast(4)}" }
        
        val bot = bot {
            token = Config.telegramToken
            logLevel = LogLevel.Network.Body // Включаем подробное логирование сети
            
            dispatch {
                command("start") {
                    logger.info { "Received /start command from ${message.chat.id}" }
                    bot.sendMessage(ChatId.fromId(message.chat.id), "Привет! Пришли мне ссылку на LinkedIn (кандидат, вакансия или компания), и я сделаю анализ.")
                }
                
                text {
                    val text = message.text
                    logger.info { "Received text message: '$text' from ${message.chat.id}" }
                    
                    if (text == null) return@text
                    if (text.startsWith("/")) return@text // Ignore commands
                    
                    val chatId = ChatId.fromId(message.chat.id)
                    
                    // Simple URL extraction
                    val urlRegex = "(https?://\\S+)".toRegex()
                    val url = urlRegex.find(text)?.value
                    
                    if (url != null) {
                        logger.info { "Detected URL: $url" }
                        bot.sendMessage(chatId, "⏳ Анализирую ссылку: $url ...")
                        scope.launch {
                            try {
                                val result = orchestrator.processLink(url)
                                logger.info { "Processing complete, sending response (length: ${result.length})" }
                                // Split message if too long (Telegram limit 4096)
                                if (result.length > 4000) {
                                    result.chunked(4000).forEach { chunk ->
                                        bot.sendMessage(chatId, chunk)
                                    }
                                } else {
                                    bot.sendMessage(chatId, result)
                                }
                            } catch (e: Exception) {
                                logger.error(e) { "Error processing link in scope" }
                                bot.sendMessage(chatId, "❌ Ошибка при обработке: ${e.message}")
                            }
                        }
                    } else {
                        logger.info { "No URL found in message" }
                        bot.sendMessage(chatId, "Пожалуйста, отправь корректную ссылку.")
                    }
                }
            }
        }
        logger.info { "Starting polling..." }
        bot.startPolling()
    }
}
