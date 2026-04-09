package com.mienaiknife.narra.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.ui.components.ArticleCarousel
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsState()
    HomeScreenContent(articles = articles)
}

@Composable
fun HomeScreenContent(
    articles: List<Article>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (articles.isNotEmpty()) {
            val continueListening = articles.filter { (it.progress ?: 0f) > 0f }.take(10)
            val newFromFeeds = articles.filter { it.isFromFeed }.take(10)
            val favorites = articles.filter { it.isFavorite }.take(10)

            if (continueListening.isNotEmpty()) {
                ArticleCarousel(
                    title = "Continue Listening",
                    articles = continueListening
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (newFromFeeds.isNotEmpty()) {
                ArticleCarousel(
                    title = "New from your feeds",
                    articles = newFromFeeds
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (favorites.isNotEmpty()) {
                ArticleCarousel(
                    title = "Your Favorites",
                    articles = favorites
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Fallback if none of the specific carousels are populated but we have articles
            if (continueListening.isEmpty() && newFromFeeds.isEmpty() && favorites.isEmpty()) {
                ArticleCarousel(
                    title = "All Articles",
                    articles = articles
                )
            }
        } else {
            Text(
                text = "No articles yet. Add some from the Add screen!",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    val mockArticles = listOf(
        Article(
            id = "1",
            title = "Modern Android Development",
            source = "Android Developers",
            content = "Content goes here...",
            progress = 0.5f
        ),
        Article(
            id = "2",
            title = "Jetpack Compose Basics",
            source = "Medium",
            content = "Content goes here..."
        )
    )
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                HomeScreenContent(articles = mockArticles)
            }
        }
    }
}
