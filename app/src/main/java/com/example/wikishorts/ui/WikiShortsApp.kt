package com.example.wikishorts.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wikishorts.R
import com.example.wikishorts.model.WikiArticle
import com.example.wikishorts.viewmodel.FeedViewModel

private enum class Tab { FEED, FAVORITES }

/** Full-width rectangular banner bar showing the app logo and name, shown above every screen. */
@Composable
private fun AppBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "WikiShorts logo",
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "WikiShorts",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

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

    // Pressing system back / gesture on the Favorites tab returns to Feed
    // instead of exiting the app.
    BackHandler(enabled = selectedTab == Tab.FAVORITES) {
        selectedTab = Tab.FEED
    }

    Scaffold(
        topBar = { AppBanner() },
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
                modifier = Modifier.padding(innerPadding),
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
