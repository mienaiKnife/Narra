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
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.ui.components.ArticleCarousel
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    onArticleClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsState()
    HomeScreenContent(
        articles = articles,
        onArticleClick = onArticleClick
    )
}

@Composable
fun HomeScreenContent(
    articles: List<Article>,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (articles.isNotEmpty()) {
            val continueListening = articles.filter { (it.progress ?: 0f) > 0f && it.isInQueue }.take(10)
            val newFromFeeds = articles.filter { it.isFromFeed && it.isInQueue }.take(10)
            val favorites = articles.filter { it.isFavorite }.take(10)

            if (continueListening.isNotEmpty()) {
                ArticleCarousel(
                    title = "Continue Listening",
                    articles = continueListening,
                    onArticleClick = onArticleClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (newFromFeeds.isNotEmpty()) {
                ArticleCarousel(
                    title = "New from your feeds",
                    articles = newFromFeeds,
                    onArticleClick = onArticleClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (favorites.isNotEmpty()) {
                ArticleCarousel(
                    title = "Your Favorites",
                    articles = favorites,
                    onArticleClick = onArticleClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Fallback if none of the specific carousels are populated but we have articles
            if (continueListening.isEmpty() && newFromFeeds.isEmpty() && favorites.isEmpty()) {
                ArticleCarousel(
                    title = "All Articles",
                    articles = articles,
                    onArticleClick = onArticleClick
                )
            }
        } else {
            Text(
                text = "No texts yet. Add some from the Add screen!",
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
    val mockArticles = SampleArticles.all
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                HomeScreenContent(
                    articles = mockArticles,
                    onArticleClick = {}
                )
            }
        }
    }
}
