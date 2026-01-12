package com.hrbot.domain

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class RecruitmentOrchestrator(
    private val contentProvider: ContentProvider,
    private val promptManager: PromptManager,
    private val linkClassifier: LinkClassifier,
    private val aiService: AIService
) {
    suspend fun processLink(url: String): String {
        val type = linkClassifier.classify(url)
        logger.info { "Classified link as $type: $url" }
        
        // 1. Get content
        val rawContent = try {
             contentProvider.getContent(url)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get content" }
            return "❌ Не удалось загрузить содержимое страницы: ${e.message}"
        }

        if (rawContent.isBlank()) {
            return "⚠️ Страница пуста или не удалось извлечь текст."
        }
        
        // 2. Get prompt
        val prompt = promptManager.getPrompt(type)
        
        // 3. Analyze
        return try {
            aiService.analyze(prompt, rawContent)
        } catch (e: Exception) {
            logger.error(e) { "AI analysis failed" }
            "❌ Ошибка анализа AI: ${e.message}"
        }
    }
}
