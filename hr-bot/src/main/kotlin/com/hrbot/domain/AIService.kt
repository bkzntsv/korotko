package com.hrbot.domain

interface AIService {
    suspend fun analyze(prompt: String, content: String): String
}
