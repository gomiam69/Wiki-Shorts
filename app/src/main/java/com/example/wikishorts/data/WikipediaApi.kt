package com.example.wikishorts.data

import com.example.wikishorts.model.WikiArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around Wikipedia's public REST API.
 * Docs: https://en.wikipedia.org/api/rest_v1/
 *
 * No API key is required for this endpoint.
 */
class WikipediaApi(
    private val languageCode: String = "en"
) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val randomSummaryUrl: String
        get() = "https://$languageCode.wikipedia.org/api/rest_v1/page/random/summary"

    /**
     * Fetches a single random Wikipedia article summary.
     * Returns null if the page has no usable extract or is a disambiguation page,
     * so callers can simply retry.
     */
    suspend fun fetchRandomArticle(): WikiArticle? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(randomSummaryUrl)
            .header("Accept", "application/json")
            .header("User-Agent", "WikiShorts/1.0 (Android app)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                parseArticle(body)
            }
        } catch (e: IOException) {
            null
        } catch (e: org.json.JSONException) {
            null
        }
    }

    /**
     * Fetches [count] unique random articles in parallel, retrying failed/duplicate/
     * low-quality picks until enough are collected or [maxAttempts] is reached.
     */
    suspend fun fetchRandomArticles(
        count: Int,
        excludingKeys: Set<String> = emptySet(),
        maxAttempts: Int = count * 3
    ): List<WikiArticle> = coroutineScope {
        val results = mutableListOf<WikiArticle>()
        val seenKeys = excludingKeys.toMutableSet()
        var attempts = 0

        while (results.size < count && attempts < maxAttempts) {
            val remaining = count - results.size
            // Fetch a batch in parallel; over-fetch slightly to account for misses.
            val batchSize = (remaining + 2).coerceAtMost(maxAttempts - attempts)
            val deferred = (0 until batchSize).map { async { fetchRandomArticle() } }
            attempts += batchSize

            deferred.mapNotNull { it.await() }.forEach { article ->
                if (results.size < count && seenKeys.add(article.key)) {
                    results.add(article)
                }
            }
        }
        results
    }

    private fun parseArticle(rawJson: String): WikiArticle? {
        val json = JSONObject(rawJson)

        val type = json.optString("type")
        if (type == "disambiguation") return null

        val extract = json.optString("extract").trim()
        if (extract.isEmpty()) return null

        val title = json.optString("title").ifBlank { json.optString("displaytitle") }
        if (title.isBlank()) return null

        // Prefer the page's main ("original") lead image — it's the actual picture shown
        // on the Wikipedia article itself. BUT: for flags, maps, logos, and coats of arms,
        // Wikipedia's "original" is often a raw .svg file straight from Commons, and the
        // thumbnail is always a safe pre-rasterized PNG/JPEG. So: use the original only when
        // it's not an .svg, otherwise fall back to the thumbnail (still decodable even with
        // the SVG decoder installed, this keeps the common case fast and reliable).
        val originalImage = json.optJSONObject("originalimage")?.optString("source")?.ifBlank { null }
        val thumbnail = json.optJSONObject("thumbnail")?.optString("source")?.ifBlank { null }
        val mainImage = when {
            originalImage != null && !originalImage.endsWith(".svg", ignoreCase = true) -> originalImage
            thumbnail != null -> thumbnail
            else -> originalImage
        }
        val description = json.optString("description").ifBlank { null }
        val pageId = json.optLong("pageid", -1L)

        val pageUrl = json.optJSONObject("content_urls")
            ?.optJSONObject("desktop")
            ?.optString("page")
            ?.ifBlank { null }
            ?: "https://$languageCode.wikipedia.org/wiki/${json.optString("titles").ifBlank { title }}"

        return WikiArticle(
            pageId = pageId,
            title = title,
            description = description,
            extract = extract,
            thumbnailUrl = mainImage,
            pageUrl = pageUrl
        )
    }
}
