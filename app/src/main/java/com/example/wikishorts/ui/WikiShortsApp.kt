package com.example.wikishorts.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wikishorts.model.WikiArticle
import com.example.wikishorts.viewmodel.FeedViewModel

private enum class Tab { FEED, FAVORITES }

@Composable
fun WikiShortsApp() {
    val feedViewModel: FeedViewModel = viewModel()
    var selectedTab by rememberSaveable { mutableStateOf(Tab.FEED) }
    // The currently opened full article, or null when showing a list/feed.
    var openedArticle by remember { mutableStateOf<WikiArticle?>(null) }

    val article = openedArticle
    if (article != null) {
        ArticleScreen(
            article = article,
            isFavorite = feedViewModel.favoritesStore.favorites
                .collectAsState().value.any { it.key == article.key },
            onToggleFavorite = { feedViewModel.toggleFavorite(article) },
            onBack = { openedArticle = null }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == Tab.FEED,
                    onClick = { selectedTab = Tab.FEED },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Feed") },
                    label = { Text("Feed") }
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.FAVORITES,
                    onClick = { selectedTab = Tab.FAVORITES },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            Tab.FEED -> FeedScreen(
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                viewModel = feedViewModel,
                onOpenArticle = { openedArticle = it }
            )
            Tab.FAVORITES -> FavoritesScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = feedViewModel,
                onOpenArticle = { openedArticle = it }
            )
        }
    }
}
