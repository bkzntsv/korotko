package com.ochemeto.di

import com.ochemeto.config.ConfigLoader
import com.ochemeto.domain.SummaryOrchestrator
import com.ochemeto.domain.SummaryOrchestratorImpl
import com.ochemeto.infrastructure.AIProvider
import com.ochemeto.infrastructure.ArticleExtractor
import com.ochemeto.infrastructure.ContentExtractor
import com.ochemeto.infrastructure.OpenAIProvider
import com.ochemeto.presentation.TelegramBotController
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    // Configuration
    single { ConfigLoader.load() }

    // HttpClient
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { 
                    ignoreUnknownKeys = true 
                    prettyPrint = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60000 // 60s for OpenAI/Scraping
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 60000
            }
        }
    }

    // Services
    single<ContentExtractor> { ArticleExtractor(get(), get()) }
    single<AIProvider> { OpenAIProvider(get<com.ochemeto.config.BotConfig>().openAiApiKey, get()) }
    single<SummaryOrchestrator> { SummaryOrchestratorImpl(get(), get()) }
    
    // Controller
    single { TelegramBotController(get(), get()) }
}

