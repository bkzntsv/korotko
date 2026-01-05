package com.ochemeto.presentation

import com.ochemeto.domain.SummarizerError
import com.ochemeto.domain.Summary

object ResponseFormatter {
    fun formatSummary(summary: Summary, wasTruncated: Boolean = false, isVoiceMessage: Boolean = false) = buildString {
        if (wasTruncated) {
            append("⚠️ _Статья очень длинная, обработана частично_\n\n")
        }
        
        if (!isVoiceMessage && summary.title != null) {
            append("📄 **${summary.title}**\n\n")
        }
        
        append("**Суть:** ${summary.mainIdea}\n\n")
        append("**Ключевые тезисы:**\n")
        summary.keyPoints.forEach { point ->
            append("• $point\n")
        }
        append("\n")
        
        if (!isVoiceMessage) {
            append("🔗 [Оригинал](${summary.originalUrl})\n\n")
        }
        
        val sentimentEmoji = when (summary.sentiment.lowercase()) {
            "positive" -> "🟢"
            "negative" -> "🔴"
            "neutral" -> "⚪"
            "technical" -> "🔧"
            "question" -> "❓"
            "request" -> "📋"
            else -> "🔵"
        }
        
        append("$sentimentEmoji **Тон:** ${summary.sentiment}")
        
        if (!isVoiceMessage && summary.clickbaitScore > 5) {
            append("  |  ⚠️ **Кликбейт:** ${summary.clickbaitScore}/10")
        }
    }
    
    fun formatError(error: SummarizerError): String = when (error) {
        is SummarizerError.NetworkError -> "🌐 **Ошибка доступа:** ${error.message}"
        is SummarizerError.ParsingError -> "📑 **Ошибка обработки:** ${error.message}"
        is SummarizerError.AIError -> "🤖 **Ошибка AI:** ${error.message}"
        is SummarizerError.ValidationError -> "⚠️ **Ошибка валидации:** ${error.message}"
        is SummarizerError.UnknownError -> "❌ **Ошибка:** Произошел сбой. Попробуйте позже."
    }
}
