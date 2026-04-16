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

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.ui.viewmodels.VoicesSettingsUiState
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.VoicesSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesSettingsScreen(
    onBack: () -> Unit,
    viewModel: VoicesSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voices") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        VoicesSettingsContent(
            uiState = uiState,
            modifier = Modifier.padding(innerPadding),
            onSetEngine = viewModel::setEngine,
            onSelectModel = viewModel::selectModel,
            onDownloadModel = viewModel::downloadModel,
            onDeleteModel = viewModel::deleteModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesSettingsContent(
    uiState: VoicesSettingsUiState,
    modifier: Modifier = Modifier,
    onSetEngine: (String) -> Unit,
    onSelectModel: (String?) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit
) {
    val context = LocalContext.current
    val engines = listOf("Android's native TTS", "On-device AI (Sherpa-ONNX)", "Cloud AI providers")
    val engineValues = listOf("android", "ondevice", "cloud")
    var expanded by remember { mutableStateOf(false) }

    val selectedEngineName = when (uiState.selectedEngine) {
        "android" -> engines[0]
        "ondevice" -> engines[1]
        "cloud" -> engines[2]
        else -> engines[0]
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Engine selection",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedEngineName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                engines.forEachIndexed { index, engine ->
                    DropdownMenuItem(
                        text = { Text(engine) },
                        onClick = {
                            onSetEngine(engineValues[index])
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState.selectedEngine) {
            "android" -> {
                Text(
                    text = "Open Android's TTS settings",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clickable {
                            try {
                                context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                            } catch (_: Exception) {
                                // Fallback or handle error
                            }
                        }
                )
            }

            "ondevice" -> {
                Text(
                    text = "Voice data",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.availableModels) { model ->
                        TtsModelItem(
                            model = model,
                            isSelected = uiState.selectedModelId == model.id,
                            onSelect = { onSelectModel(model.id) },
                            onDownload = { onDownloadModel(model.id) },
                            onDelete = { onDeleteModel(model.id) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun TtsModelItem(
    model: TtsModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val isDownloading = model.progress > 0f && model.progress < 1f

    ListItem(
        modifier = Modifier.clickable(enabled = model.isDownloaded && !isDownloading) { onSelect() },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (model.isDownloaded || isDownloading) MaterialTheme.colorScheme.onSurface 
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "(Selected)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        supportingContent = {
            Column(modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = "${model.language} • ${model.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { model.progress },
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        },
        trailingContent = {
            when {
                model.isDownloaded -> {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete model",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                isDownloading -> {
                    // Redundant circular indicator removed for cleaner UI during download
                }
                else -> {
                    IconButton(onClick = onDownload) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download model",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun VoicesSettingsScreenPreview() {
    val navController = rememberNavController()
    val mockUiState = VoicesSettingsUiState(
        availableModels = listOf(
            TtsModel(
                id = "1",
                name = "English (US) - VITS",
                language = "en-US",
                description = "High quality VITS model",
                type = TtsModelType.VITS,
                modelUrl = "",
                tokensUrl = "",
                isDownloaded = true
            ),
            TtsModel(
                id = "2",
                name = "English (UK) - Matcha",
                language = "en-GB",
                description = "Fast Matcha model",
                type = TtsModelType.MATCHA,
                modelUrl = "",
                tokensUrl = "",
                isDownloaded = false,
                progress = 0.5f
            )
        ),
        selectedEngine = "ondevice",
        selectedModelId = "1"
    )

    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Voices") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            VoicesSettingsContent(
                uiState = mockUiState,
                modifier = Modifier.padding(innerPadding),
                onSetEngine = {},
                onSelectModel = {},
                onDownloadModel = {},
                onDeleteModel = {}
            )
        }
    }
}
