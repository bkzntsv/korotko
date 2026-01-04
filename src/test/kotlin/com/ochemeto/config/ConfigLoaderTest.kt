package com.ochemeto.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ConfigLoaderTest : StringSpec({

    "load should return config when required env vars are present" {
        val envMap = mapOf(
            "TELEGRAM_BOT_TOKEN" to "test_token",
            "OPENAI_API_KEY" to "test_key"
        )
        
        val config = ConfigLoader.load { envMap[it] }
        
        config.telegramToken shouldBe "test_token"
        config.openAiApiKey shouldBe "test_key"
    }

    "load should throw exception when TELEGRAM_BOT_TOKEN is missing" {
        val envMap = mapOf("OPENAI_API_KEY" to "test_key")
        
        shouldThrow<ConfigurationException> {
            ConfigLoader.load { envMap[it] }
        }.message shouldBe "TELEGRAM_BOT_TOKEN not set"
    }

    "load should throw exception when OPENAI_API_KEY is missing" {
        val envMap = mapOf("TELEGRAM_BOT_TOKEN" to "test_token")
        
        shouldThrow<ConfigurationException> {
            ConfigLoader.load { envMap[it] }
        }.message shouldBe "OPENAI_API_KEY not set"
    }
    
    "load should use custom USER_AGENT if provided" {
        val envMap = mapOf(
            "TELEGRAM_BOT_TOKEN" to "test_token",
            "OPENAI_API_KEY" to "test_key",
            "USER_AGENT" to "CustomAgent/1.0"
        )
        
        val config = ConfigLoader.load { envMap[it] }
        
        config.userAgent shouldBe "CustomAgent/1.0"
    }
})

