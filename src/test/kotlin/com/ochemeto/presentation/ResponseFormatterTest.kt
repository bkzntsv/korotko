package com.ochemeto.presentation

import com.ochemeto.domain.SummarizerError
import com.ochemeto.domain.Summary
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ResponseFormatterTest : StringSpec({

    "formatSummary should include all required fields" {
        val summary = Summary(
            mainIdea = "Main Idea",
            keyPoints = listOf("Point 1", "Point 2"),
            sentiment = "Positive",
            clickbaitScore = 2,
            tags = listOf("AI", "Tech"),
            title = "Article Title",
            originalUrl = "http://example.com",
            wasTruncated = false
        )
        
        val result = ResponseFormatter.formatSummary(summary, wasTruncated = false)
        
        result shouldContain "📄 **Article Title**"
        result shouldContain "🟢 **Тон:** Positive"
        result shouldContain "**Суть:** Main Idea"
        result shouldContain "• Point 1"
        result shouldContain "#AI #Tech"
        result shouldContain "🔗 [Оригинал](http://example.com)"
        // Clickbait score <= 5 should be hidden
        result shouldNotContain "⚠️ **Кликбейт:**"
    }
    
    "formatSummary should show clickbait warning if score > 5" {
        val summary = Summary(
            mainIdea = "Idea", 
            keyPoints = listOf("P1"), 
            sentiment = "Neutral",
            clickbaitScore = 8,
            tags = emptyList(),
            title = "Title", 
            originalUrl = "url", 
            wasTruncated = true
        )
        
        val result = ResponseFormatter.formatSummary(summary, wasTruncated = true)
        
        result shouldContain "⚠️ **Кликбейт:** 8/10"
        result shouldContain "⚠️ _Статья очень длинная, обработана частично_"
    }
})
