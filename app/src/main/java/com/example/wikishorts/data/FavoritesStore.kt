package com.example.wikishorts.data

import android.content.Context
import com.example.wikishorts.model.WikiArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the user's favourite ("blue heart") articles to disk so they
 * survive app restarts. Backed by SharedPreferences + a JSON array — no
 * database setup required, which keeps the app simple and reliably compilable.
 */
class FavoritesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow<List<WikiArticle>>(loadFromDisk())
    val favorites: StateFlow<List<WikiArticle>> = _favorites.asStateFlow()

    fun isFavorite(article: WikiArticle): Boolean =
        _favorites.value.any { it.key == article.key }

    suspend fun toggleFavorite(article: WikiArticle) = withContext(Dispatchers.IO) {
        val current = _favorites.value
        val updated = if (current.any { it.key == article.key }) {
            current.filterNot { it.key == article.key }
        } else {
            current + article
        }
        _favorites.value = updated
        persist(updated)
    }

    private fun loadFromDisk(): List<WikiArticle> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                WikiArticle(
                    pageId = obj.optLong("pageId", -1L),
                    title = obj.optString("title"),
                    description = obj.optString("description").ifBlank { null },
                    extract = obj.optString("extract"),
                    thumbnailUrl = obj.optString("thumbnailUrl").ifBlank { null },
                    pageUrl = obj.optString("pageUrl")
                )
            }
        } catch (e: org.json.JSONException) {
            emptyList()
        }
    }

    private fun persist(articles: List<WikiArticle>) {
        val array = JSONArray()
        articles.forEach { article ->
            val obj = JSONObject()
            obj.put("pageId", article.pageId)
            obj.put("title", article.title)
            obj.put("description", article.description ?: "")
            obj.put("extract", article.extract)
            obj.put("thumbnailUrl", article.thumbnailUrl ?: "")
            obj.put("pageUrl", article.pageUrl)
            array.put(obj)
        }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "wikishorts_favorites"
        private const val KEY_FAVORITES = "favorites_json"
    }
}
