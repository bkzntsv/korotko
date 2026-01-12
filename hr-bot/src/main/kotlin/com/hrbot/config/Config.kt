package com.hrbot.config

import java.io.File
import java.util.Properties

object Config {
    private val env = loadEnv()

    val telegramToken: String = System.getenv("TELEGRAM_BOT_TOKEN") ?: env["TELEGRAM_BOT_TOKEN"] ?: "dummy_token"
    val openAiToken: String = System.getenv("OPENAI_API_KEY") ?: env["OPENAI_API_KEY"] ?: "dummy_token"
    val zenRowsKey: String = System.getenv("ZENROWS_API_KEY") ?: env["ZENROWS_API_KEY"] ?: "dummy_key"

    private fun loadEnv(): Map<String, String> {
        val envFile = File("hr-bot/src/main/.env")
        if (envFile.exists()) {
            return envFile.readLines()
                .filter { it.contains("=") && !it.startsWith("#") }
                .associate { line ->
                    val parts = line.split("=", limit = 2)
                    parts[0].trim() to parts[1].trim()
                }
        }
        return emptyMap()
    }
}
