package com.example.wikishorts.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wikishorts.data.FavoritesStore
import com.example.wikishorts.data.WikipediaApi
import com.example.wikishorts.model.WikiArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val articles: List<WikiArticle> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefilling: Boolean = false,
    val error: String? = null
)

/**
 * Keeps a rolling buffer of [BUFFER_SIZE] random Wikipedia articles ready to swipe
 * through, transparently fetching more from the API as the user approaches the end.
 */
class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val api = WikipediaApi()
    val favoritesStore = FavoritesStore(application)

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var isFetchingMore = false

    init {
        loadInitialBuffer()
    }

    private fun loadInitialBuffer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true, error = null) }
            val articles = api.fetchRandomArticles(BUFFER_SIZE)
            _uiState.update {
                it.copy(
                    articles = articles,
                    isInitialLoading = false,
                    error = if (articles.isEmpty()) {
                        "Couldn't load articles. Check your internet connection and try again."
                    } else null
                )
            }
        }
    }

    fun retryInitialLoad() = loadInitialBuffer()

    /** Called by the pager as pages scroll by so we can top up the buffer just in time. */
    fun onPageVisible(index: Int) {
        val remaining = _uiState.value.articles.size - 1 - index
        if (remaining <= REFILL_THRESHOLD && !isFetchingMore) {
            fetchMoreArticles()
        }
    }

    private fun fetchMoreArticles() {
        isFetchingMore = true
        _uiState.update { it.copy(isRefilling = true) }
        viewModelScope.launch {
            val existingKeys = _uiState.value.articles.map { it.key }.toSet()
            val newArticles = api.fetchRandomArticles(
                count = REFILL_BATCH,
                excludingKeys = existingKeys
            )
            _uiState.update {
                it.copy(articles = it.articles + newArticles, isRefilling = false)
            }
            isFetchingMore = false
        }
    }

    fun toggleFavorite(article: WikiArticle) {
        viewModelScope.launch { favoritesStore.toggleFavorite(article) }
    }

    companion object {
        private const val BUFFER_SIZE = 20
        private const val REFILL_THRESHOLD = 5
        private const val REFILL_BATCH = 10
    }
}
