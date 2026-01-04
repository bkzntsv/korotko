package com.ochemeto

import com.ochemeto.di.appModule
import com.ochemeto.presentation.TelegramBotController
import io.ktor.client.HttpClient
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.java.KoinJavaComponent.get
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting AI Content Summarizer Bot..." }

    try {
        val koinApp = startKoin {
            modules(appModule)
        }

        val controller = koinApp.koin.get<TelegramBotController>()
        
        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info { "Shutting down..." }
            val client = koinApp.koin.get<HttpClient>()
            client.close()
            stopKoin()
            logger.info { "Shutdown complete." }
        })

        controller.start()
        
    } catch (e: Exception) {
        logger.error(e) { "Fatal error starting application" }
        System.exit(1)
    }
}

