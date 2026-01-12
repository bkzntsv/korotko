package com.hrbot.domain

enum class LinkType {
    CANDIDATE,
    JOB,
    COMPANY,
    UNKNOWN
}

class LinkClassifier {
    fun classify(url: String): LinkType {
        return when {
            url.contains("/in/") -> LinkType.CANDIDATE
            url.contains("/jobs/") || url.contains("/view/") -> LinkType.JOB // LinkedIn job pattern
            url.contains("/company/") -> LinkType.COMPANY
            else -> LinkType.UNKNOWN
        }
    }
}
