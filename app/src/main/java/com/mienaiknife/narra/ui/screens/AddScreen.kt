package com.mienaiknife.narra.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.HomeViewModel

@Composable
fun AddScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onArticleAdded: () -> Unit = {}
) {
    AddScreenContent(
        onDownloadClick = { url -> viewModel.downloadArticle(url) },
        onSubscribeClick = { url -> viewModel.subscribeToFeed(url) },
        onArticleAdded = onArticleAdded
    )
}

@Composable
fun AddScreenContent(
    onDownloadClick: (String) -> Unit,
    onSubscribeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onArticleAdded: () -> Unit = {}
) {
    var url by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Add",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Article URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (url.isNotBlank()) {
                    onDownloadClick(url)
                    onArticleAdded()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(100),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Download for Offline Reading",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (url.isNotBlank()) {
                    onSubscribeClick(url)
                    onArticleAdded()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(100),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                text = "Subscribe to RSS Feed",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun AddScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AddScreenContent(
                    onDownloadClick = {},
                    onSubscribeClick = {}
                )
            }
        }
    }
}
