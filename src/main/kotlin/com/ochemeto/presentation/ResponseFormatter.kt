package com.ochemeto.presentation

import com.ochemeto.domain.SummarizerError
import com.ochemeto.domain.Summary

object ResponseFormatter {
    fun formatSummary(summary: Summary, wasTruncated: Boolean = false) = buildString {
        append("📄 **${summary.title ?: "Без названия"}**\n\n")
        
        // Metadata Line: Sentiment & Clickbait
        val sentimentEmoji = when (summary.sentiment.lowercase()) {
            "positive" -> "🟢"
            "negative" -> "🔴"
            "neutral" -> "⚪"
            "technical" -> "🔧"
            else -> "🔵"
        }
        
        append("$sentimentEmoji **Тон:** ${summary.sentiment}")
        
        if (summary.clickbaitScore > 5) {
            append("  |  ⚠️ **Кликбейт:** ${summary.clickbaitScore}/10")
        }
        append("\n\n")

        append("**Суть:** ${summary.mainIdea}\n\n")
        
        append("**Ключевые тезисы:**\n")
        summary.keyPoints.forEach { point ->
            append("• $point\n")
        }
        append("\n")
        
        // Tags
        if (summary.tags.isNotEmpty()) {
            append(summary.tags.joinToString(" ") { "#${it.replace(" ", "_")}" })
            append("\n\n")
        }
        
        if (wasTruncated) {
            append("⚠️ _Статья очень длинная, обработана частично_\n\n")
        }
        
        append("🔗 [Оригинал](${summary.originalUrl})")
    }
    
    fun formatError(error: SummarizerError): String = when (error) {
        is SummarizerError.NetworkError -> "🌐 **Ошибка доступа:** ${error.message}"
        is SummarizerError.ParsingError -> "📑 **Ошибка обработки:** ${error.message}"
        is SummarizerError.AIError -> "🤖 **Ошибка AI:** ${error.message}"
        is SummarizerError.ValidationError -> "⚠️ **Ошибка валидации:** ${error.message}"
        is SummarizerError.UnknownError -> "❌ **Ошибка:** Произошел сбой. Попробуйте позже."
    }
}
