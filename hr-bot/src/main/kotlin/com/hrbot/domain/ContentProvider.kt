package com.hrbot.domain

interface ContentProvider {
    suspend fun getContent(url: String): String
}
