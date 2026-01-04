package com.ochemeto.config

data class BotConfig(
    val telegramToken: String,
    val openAiApiKey: String,
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
)

class ConfigurationException(message: String) : RuntimeException(message)

object ConfigLoader {
    // Interface for env provider to make it testable
    fun interface EnvProvider {
        fun get(name: String): String?
    }

    private val defaultEnvProvider = EnvProvider { System.getenv(it) }

    fun load(envProvider: EnvProvider = defaultEnvProvider): BotConfig {
        val telegramToken = envProvider.get("TELEGRAM_BOT_TOKEN")
            ?: throw ConfigurationException("TELEGRAM_BOT_TOKEN not set")
            
        val openAiApiKey = envProvider.get("OPENAI_API_KEY")
            ?: throw ConfigurationException("OPENAI_API_KEY not set")
            
        val userAgent = envProvider.get("USER_AGENT") 
            ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        return BotConfig(
            telegramToken = telegramToken,
            openAiApiKey = openAiApiKey,
            userAgent = userAgent
        )
    }
}

