/*
 * Copyright 2025 Narra Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mienaiknife.narra.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.theme.NarraTheme

@Composable
fun SettingsScreen(
    onNavigateToUserInterface: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToVoices: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToAbout: () -> Unit,
    navigateToDestination: (com.mienaiknife.narra.NavDestination) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        var expanded by remember { mutableStateOf(false) }
        val resources = LocalResources.current

        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    expanded = it.isNotBlank()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                placeholder = { Text(stringResource(R.string.settings_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            expanded = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.action_clear)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            val filteredSettings = remember(searchQuery, resources) {
                if (searchQuery.isBlank()) {
                    emptyList()
                } else {
                    allSearchableSettings.filter { item ->
                        // Using stringResource is not possible here as it's not a composable context (inside remember's lambda)
                        // but actually, remember's lambda is NOT a composable context, but we are inside a Composable function.
                        // However, allSearchableSettings uses resource IDs. We can use resources.getString here.
                        // To avoid the lint warning, we can suppress it or just accept it's necessary here.
                        resources.getString(item.titleRes).contains(searchQuery, ignoreCase = true) ||
                                resources.getString(item.subtitleRes).contains(searchQuery, ignoreCase = true) ||
                                item.keywords.any { it.contains(searchQuery, ignoreCase = true) }
                    }.take(5)
                }
            }

            if (expanded && filteredSettings.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredSettings.forEach { result ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(result.titleRes), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        stringResource(result.subtitleRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                navigateToDestination(result.destination)
                                searchQuery = ""
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        val settingsItems = listOf(
            SettingsItem(
                title = stringResource(R.string.settings_ui_title),
                subtitle = stringResource(R.string.settings_ui_subtitle),
                icon = Icons.Default.StayCurrentPortrait,
                onClick = onNavigateToUserInterface
            ),
            SettingsItem(
                title = stringResource(R.string.settings_playback_title),
                subtitle = stringResource(R.string.settings_playback_subtitle),
                icon = Icons.Default.Headphones,
                onClick = onNavigateToPlayback
            ),
            SettingsItem(
                title = stringResource(R.string.settings_voices_title),
                subtitle = stringResource(R.string.settings_voices_subtitle),
                icon = Icons.Default.RecordVoiceOver, 
                onClick = onNavigateToVoices
            ),
            SettingsItem(
                title = stringResource(R.string.settings_downloads_title),
                subtitle = stringResource(R.string.settings_downloads_subtitle),
                icon = Icons.Default.Download,
                onClick = onNavigateToDownloads
            ),
            SettingsItem(
                title = stringResource(R.string.settings_about_title),
                subtitle = stringResource(R.string.settings_about_subtitle),
                icon = Icons.Default.Info,
                onClick = onNavigateToAbout
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(settingsItems) { item ->
                SettingsListItem(item)
            }
        }
    }
}

data class SettingsItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun SettingsListItem(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                SettingsScreen(
                    onNavigateToUserInterface = {},
                    onNavigateToPlayback = {},
                    onNavigateToVoices = {},
                    onNavigateToDownloads = {},
                    onNavigateToAbout = {},
                    navigateToDestination = {}
                )
            }
        }
    }
}
