package com.hrbot

import com.hrbot.config.Config
import com.hrbot.domain.LinkClassifier
import com.hrbot.domain.PromptManager
import com.hrbot.domain.RecruitmentOrchestrator
import com.hrbot.infrastructure.OpenAIWrapper
import com.hrbot.infrastructure.ZenRowsScraper
import com.hrbot.presentation.BotController
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

fun main() {
    val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000 // 2 minutes
            connectTimeoutMillis = 60_000  // 1 minute
            socketTimeoutMillis = 120_000  // 2 minutes
        }
    }
    
    val zenRowsScraper = ZenRowsScraper(Config.zenRowsKey, httpClient)
    val promptManager = PromptManager()
    val linkClassifier = LinkClassifier()
    val aiService = OpenAIWrapper(Config.openAiToken)
    
    val orchestrator = RecruitmentOrchestrator(
        contentProvider = zenRowsScraper,
        promptManager = promptManager,
        linkClassifier = linkClassifier,
        aiService = aiService
    )
    
    val botController = BotController(orchestrator)
    println("Starting HR Bot...")
    botController.start()
}
