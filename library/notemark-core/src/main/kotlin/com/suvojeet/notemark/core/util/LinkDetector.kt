package com.suvojeet.notemark.core.util

/**
 * Utility for detecting URLs in text.
 */
object LinkDetector {
    private val urlRegex = "(https?://[\\w.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?)".toRegex()

    /**
     * Finds all unique URLs in the given text.
     */
    fun detectUrls(text: String): Set<String> {
        return urlRegex.findAll(text).map { it.value }.toSet()
    }
}
