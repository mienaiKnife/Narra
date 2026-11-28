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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.NarraListItem
import com.mienaiknife.narra.ui.components.NarraTextFieldShape
import com.mienaiknife.narra.ui.components.TopLevelScreen
import com.mienaiknife.narra.ui.components.narraTextFieldColors
import com.mienaiknife.narra.ui.theme.LocalNarraSpacing
import com.mienaiknife.narra.ui.theme.NarraTheme

@Composable
fun SettingsScreen(
    onNavigateToUserInterface: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToVoices: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToAbout: () -> Unit,
    navigateToDestination: (com.mienaiknife.narra.NavDestination) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }

    TopLevelScreen(title = stringResource(R.string.nav_settings)) {
        var expanded by remember { mutableStateOf(false) }
        val resources = LocalResources.current

        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    expanded = it.isNotBlank()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable),
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
                                contentDescription = stringResource(R.string.action_clear),
                            )
                        }
                    }
                },
                shape = NarraTextFieldShape,
                colors = narraTextFieldColors(),
                singleLine = true,
            )

            val filteredSettings = remember(searchQuery, resources) {
                if (searchQuery.isBlank()) {
                    emptyList()
                } else {
                    allSearchableSettings.filter { item ->
                        // resources.getString is used instead of stringResource because this runs
                        // inside remember, which is not a composable context.
                        resources.getString(item.titleRes).contains(searchQuery, ignoreCase = true) ||
                            resources.getString(item.subtitleRes).contains(searchQuery, ignoreCase = true) ||
                            item.keywordsRes.let { keywordsRes ->
                                resources.getStringArray(keywordsRes).any { it.contains(searchQuery, ignoreCase = true) }
                            }
                    }.take(5)
                }
            }

            if (expanded && filteredSettings.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    filteredSettings.forEach { result ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical)) {
                                    Text(stringResource(result.titleRes), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        stringResource(result.subtitleRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                navigateToDestination(result.destination)
                                searchQuery = ""
                                expanded = false
                            },
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
                onClick = onNavigateToUserInterface,
            ),
            SettingsItem(
                title = stringResource(R.string.settings_playback_title),
                subtitle = stringResource(R.string.settings_playback_subtitle),
                icon = Icons.Default.Headphones,
                onClick = onNavigateToPlayback,
            ),
            SettingsItem(
                title = stringResource(R.string.settings_voices_title),
                subtitle = stringResource(R.string.settings_voices_subtitle),
                icon = Icons.Default.RecordVoiceOver,
                onClick = onNavigateToVoices,
            ),
            SettingsItem(
                title = stringResource(R.string.settings_downloads_title),
                subtitle = stringResource(R.string.settings_downloads_subtitle),
                icon = Icons.Default.Download,
                onClick = onNavigateToDownloads,
            ),
            SettingsItem(
                title = stringResource(R.string.settings_about_title),
                subtitle = stringResource(R.string.settings_about_subtitle),
                icon = Icons.Default.Info,
                onClick = onNavigateToAbout,
            ),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(settingsItems, key = { it.title }) { item ->
                NarraListItem(
                    title = item.title,
                    subtitle = item.subtitle,
                    leadingIcon = item.icon,
                    onClick = item.onClick,
                )
            }
        }
    }
}

data class SettingsItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@PreviewLightDark
@Composable
fun SettingsScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                SettingsScreen(
                    onNavigateToUserInterface = {},
                    onNavigateToPlayback = {},
                    onNavigateToVoices = {},
                    onNavigateToDownloads = {},
                    onNavigateToAbout = {},
                    navigateToDestination = {},
                )
            }
        }
    }
}
