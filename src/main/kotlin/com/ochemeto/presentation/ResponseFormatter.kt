package com.ochemeto.presentation

import com.ochemeto.domain.SummarizerError
import com.ochemeto.domain.Summary

object ResponseFormatter {
    fun formatSummary(summary: Summary, wasTruncated: Boolean = false) = buildString {
        // Предупреждение о частичной обработке (если было)
        if (wasTruncated) {
            append("⚠️ _Статья очень длинная, обработана частично_\n\n")
        }
        
        // Заголовок статьи
        append("📄 **${summary.title ?: "Без названия"}**\n\n")
        
        // Суть
        append("**Суть:** ${summary.mainIdea}\n\n")
        
        // Ключевые тезисы
        append("**Ключевые тезисы:**\n")
        summary.keyPoints.forEach { point ->
            append("• $point\n")
        }
        append("\n")
        
        // Ссылка на оригинал
        append("🔗 [Оригинал](${summary.originalUrl})\n\n")
        
        // Тон статьи и оценка кликбейта
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
    }
    
    fun formatError(error: SummarizerError): String = when (error) {
        is SummarizerError.NetworkError -> "🌐 **Ошибка доступа:** ${error.message}"
        is SummarizerError.ParsingError -> "📑 **Ошибка обработки:** ${error.message}"
        is SummarizerError.AIError -> "🤖 **Ошибка AI:** ${error.message}"
        is SummarizerError.ValidationError -> "⚠️ **Ошибка валидации:** ${error.message}"
        is SummarizerError.UnknownError -> "❌ **Ошибка:** Произошел сбой. Попробуйте позже."
    }
}
