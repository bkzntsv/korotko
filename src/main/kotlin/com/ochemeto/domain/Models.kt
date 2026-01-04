package com.ochemeto.domain

data class ExtractedContent(
    val text: String,
    val title: String?,
    val url: String,
    val wasTruncated: Boolean = false
)

data class Summary(
    val mainIdea: String,
    val keyPoints: List<String>,
    val sentiment: String,
    val clickbaitScore: Int,
    val tags: List<String>,
    val title: String?,
    val originalUrl: String,
    val wasTruncated: Boolean = false
)

sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val error: SummarizerError) : Result<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure
    
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
}

sealed class SummarizerError {
    data class NetworkError(val message: String) : SummarizerError()
    data class ParsingError(val message: String) : SummarizerError()
    data class AIError(val message: String) : SummarizerError()
    data class ValidationError(val message: String) : SummarizerError()
    data class UnknownError(val throwable: Throwable) : SummarizerError()
}
