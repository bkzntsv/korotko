package com.ochemeto

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.ochemeto.config.ConfigLoader
import com.ochemeto.di.appModule
import com.ochemeto.di.voiceModule
import com.ochemeto.domain.voice.VoiceTranscriptionOrchestrator
import com.ochemeto.presentation.cancelBotControllerScope
import com.ochemeto.presentation.setupTextHandlers
import com.ochemeto.presentation.setupVoiceHandlers
import io.ktor.client.HttpClient
import mu.KotlinLogging
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting AI Content Summarizer Bot..." }

    try {
        // 1. Загружаем конфиг
        val config = ConfigLoader.load()

        // 2. Стартуем Koin только с appModule (без voiceModule, т.к. ему нужен bot)
        val koinApp = startKoin {
            modules(appModule)
        }

        // 3. Создаем бота (Presentation layer)
        val bot = bot {
            token = config.telegramToken
            dispatch {
                // Подключаем текстовые хендлеры
                val orchestrator = GlobalContext.get().get<com.ochemeto.domain.SummaryOrchestrator>()
                setupTextHandlers(orchestrator)
                
                // Подключаем голосовые хендлеры с ленивой инициализацией зависимостей
                val voiceOrchestratorProvider = { GlobalContext.get().get<VoiceTranscriptionOrchestrator>() }
                setupVoiceHandlers(voiceOrchestratorProvider)
            }
        }
        
        // 4. Регистрируем инстанс бота в Koin
        org.koin.core.context.loadKoinModules(org.koin.dsl.module {
            single { bot }
        })
        
        // 5. Загружаем voiceModule после регистрации bot
        org.koin.core.context.loadKoinModules(voiceModule)
        
        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info { "Shutting down..." }
            cancelBotControllerScope()
            val client = koinApp.koin.get<HttpClient>()
            client.close()
            stopKoin()
            logger.info { "Shutdown complete." }
        })
        
        logger.info { "Bot started polling" }
        bot.startPolling()
        
    } catch (e: Exception) {
        logger.error(e) { "Fatal error starting application" }
        System.exit(1)
    }
}
