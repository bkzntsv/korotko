package com.ochemeto.di

import com.aallam.openai.client.OpenAI
import com.ochemeto.config.BotConfig
import com.ochemeto.config.ConfigLoader
import com.ochemeto.domain.SummaryOrchestrator
import com.ochemeto.domain.SummaryOrchestratorImpl
import com.ochemeto.domain.voice.VoiceTranscriptionConfig
import com.ochemeto.domain.voice.VoiceTranscriptionOrchestrator
import com.ochemeto.domain.voice.VoiceTranscriptionOrchestratorImpl
import com.ochemeto.infrastructure.AIProvider
import com.ochemeto.infrastructure.ArticleExtractor
import com.ochemeto.infrastructure.ContentExtractor
import com.ochemeto.infrastructure.OpenAIProvider
import com.ochemeto.infrastructure.voice.FileDownloader
import com.ochemeto.infrastructure.voice.OpenAiTranscriptionProvider
import com.ochemeto.infrastructure.voice.TelegramFileDownloader
import com.ochemeto.infrastructure.voice.TranscriptionProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    single { ConfigLoader.load() }
    single { VoiceTranscriptionConfig() }

    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { 
                    ignoreUnknownKeys = true 
                    prettyPrint = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 60000
            }
        }
    }
    
    single {
        OpenAI(token = get<BotConfig>().openAiApiKey)
    }

    single<ContentExtractor> { ArticleExtractor(get(), get()) }
    single<AIProvider> { OpenAIProvider(get<BotConfig>().openAiApiKey, get()) }
    single<SummaryOrchestrator> { SummaryOrchestratorImpl(get(), get()) }
}

val voiceModule = module {
    single<FileDownloader> { 
        TelegramFileDownloader(
            bot = get<com.github.kotlintelegrambot.Bot>(),
            token = get<BotConfig>().telegramToken, 
            httpClient = get() 
        ) 
    }
    
    single<TranscriptionProvider> { 
        OpenAiTranscriptionProvider(
            openAiClient = get(),
            model = get<VoiceTranscriptionConfig>().whisperModel,
            language = get<VoiceTranscriptionConfig>().defaultLanguage
        )
    }

    single<VoiceTranscriptionOrchestrator> { 
        VoiceTranscriptionOrchestratorImpl(
            fileDownloader = get(),
            transcriptionProvider = get(),
            summaryOrchestrator = get()
        ) 
    }
}
