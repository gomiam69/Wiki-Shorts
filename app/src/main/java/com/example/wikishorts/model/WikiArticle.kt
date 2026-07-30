package com.example.wikishorts.model

/**
 * Represents a single Wikipedia article as shown on a reel card,
 * and (if opened) as a full article in the in-app reader.
 */
data class WikiArticle(
    val pageId: Long,
    val title: String,
    val description: String?,
    val extract: String,
    val thumbnailUrl: String?,
    val pageUrl: String
) {
    /** Stable identity used for de-duplication inside the feed buffer and favourites. */
    val key: String get() = pageUrl.ifBlank { "$pageId:$title" }
}
