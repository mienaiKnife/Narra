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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.ui.viewmodels.VoicesSettingsUiState
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.VoicesSettingsViewModel

@Composable
fun VoicesSettingsScreen(
    onBack: () -> Unit,
    viewModel: VoicesSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    VoicesSettingsContent(
        uiState = uiState,
        onSetEngine = viewModel::setEngine,
        onSelectModel = viewModel::selectModel,
        onSetSpeakerId = viewModel::setSpeakerId,
        onDownloadModel = viewModel::downloadModel,
        onDeleteModel = viewModel::deleteModel,
        onSetSherpaNoiseScale = viewModel::setSherpaNoiseScale,
        onSetSherpaLengthScale = viewModel::setSherpaLengthScale,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesSettingsContent(
    uiState: VoicesSettingsUiState,
    modifier: Modifier = Modifier,
    onSetEngine: (String) -> Unit,
    onSelectModel: (String?) -> Unit,
    onSetSpeakerId: (Int) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onSetSherpaNoiseScale: (Float) -> Unit,
    onSetSherpaLengthScale: (Float) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engines = listOf("Android's native TTS", "On-device AI (Sherpa-ONNX)")
    val engineValues = listOf("android", "ondevice")
    var expanded by remember { mutableStateOf(false) }

    val kokoroVoices = listOf(
        "Amelie (Female)" to 5,
        "Bella (Female)" to 1,
        "Michael (Male)" to 6,
        "Sarah (Female)" to 2,
        "Nicole (Female)" to 3,
        "Sky (Female)" to 4,
        "George (Male)" to 7,
        "Lewis (Male)" to 8,
        "Alice (Female)" to 9,
        "Lily (Female)" to 10,
        "Julia (Female)" to 0
    ).sortedBy { it.first }
    var kokoroExpanded by remember { mutableStateOf(false) }

    val selectedEngineName = when (uiState.selectedEngine) {
        "android" -> engines[0]
        "ondevice" -> engines[1]
        else -> engines[0]
    }

    val isInitializing = uiState.engineState is TtsState.Initializing
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        if (isInitializing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Voices",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Engine selection",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded && !isInitializing,
                onExpandedChange = { if (!isInitializing) expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedEngineName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isInitializing,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = !isInitializing)
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
                    val sliderColors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    )

                    Text(
                        text = "Voice settings",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = "Noise Scale (Expressiveness): ${"%.3f".format(uiState.sherpaNoiseScale)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Slider(
                            value = uiState.sherpaNoiseScale,
                            onValueChange = onSetSherpaNoiseScale,
                            enabled = !isInitializing,
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = sliderColors,
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    colors = sliderColors,
                                    drawStopIndicator = null
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Length Scale: ${"%.2f".format(uiState.sherpaLengthScale)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Slider(
                            value = uiState.sherpaLengthScale,
                            onValueChange = onSetSherpaLengthScale,
                            enabled = !isInitializing,
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = sliderColors,
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    colors = sliderColors,
                                    drawStopIndicator = null
                                )
                            }
                        )
                    }

                    val selectedModel = uiState.availableModels.find { it.id == uiState.selectedModelId }
                    if (selectedModel?.type == TtsModelType.KOKORO) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Kokoro Voice",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        val currentVoice = kokoroVoices.find { it.second == uiState.selectedSpeakerId }?.first ?: "Unknown"

                        ExposedDropdownMenuBox(
                            expanded = kokoroExpanded && !isInitializing,
                            onExpandedChange = { if (!isInitializing) kokoroExpanded = !kokoroExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentVoice,
                                onValueChange = {},
                                readOnly = true,
                                enabled = !isInitializing,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kokoroExpanded) },
                                modifier = Modifier
                                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = !isInitializing)
                                    .fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = kokoroExpanded,
                                onDismissRequest = { kokoroExpanded = false }
                            ) {
                                kokoroVoices.forEach { (name, id) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            onSetSpeakerId(id)
                                            kokoroExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Voice data",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            uiState.availableModels.forEachIndexed { index, model ->
                                TtsModelItem(
                                    model = model,
                                    isSelected = uiState.selectedModelId == model.id,
                                    onSelect = { if (!isInitializing) onSelectModel(model.id) },
                                    onDownload = { onDownloadModel(model.id) },
                                    onDelete = { onDeleteModel(model.id) },
                                    containerColor = Color.Transparent,
                                    enabled = !isInitializing
                                )
                                if (index < uiState.availableModels.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
    onDelete: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true
) {
    val isDownloading = model.progress > 0f && model.progress < 1f

    ListItem(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clickable(enabled = model.isDownloaded && !isDownloading && enabled) { onSelect() },
        colors = ListItemDefaults.colors(
            containerColor = containerColor
        ),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = StrokeCap.Butt,
                        gapSize = 5.dp,
                        drawStopIndicator = {}
                    )
                }
            }
        },
        trailingContent = {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    model.isDownloaded -> {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete model",
                                tint = MaterialTheme.colorScheme.onSurface
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
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun VoicesSettingsScreenPreview() {
    val navController = rememberNavController()
    val mockUiState = VoicesSettingsUiState(
        availableModels = listOf(
            TtsModel(
                id = "vits-piper-en_US-amy-low",
                name = "Piper Amy (English, US)",
                language = "en-US",
                description = "Low quality, fast American English female voice",
                type = TtsModelType.VITS,
                modelUrl = "",
                tokensUrl = "",
                isDownloaded = true
            ),
            TtsModel(
                id = "vits-piper-en_US-ryan-medium",
                name = "Piper Ryan (English, US)",
                language = "en-US",
                description = "Medium quality American English male voice",
                type = TtsModelType.VITS,
                modelUrl = "",
                tokensUrl = "",
                isDownloaded = false
            ),
            TtsModel(
                id = "matcha-en-ljspeech",
                name = "Matcha (English)",
                language = "en",
                description = "High quality Matcha TTS model",
                type = TtsModelType.MATCHA,
                modelUrl = "",
                tokensUrl = "",
                isDownloaded = false,
                progress = 0.5f
            )
        ),
        selectedEngine = "ondevice",
        selectedModelId = "vits-piper-en_US-amy-low"
    )

    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                VoicesSettingsContent(
                    uiState = mockUiState,
                    onSetEngine = {},
                    onSelectModel = {},
                    onSetSpeakerId = {},
                    onDownloadModel = {},
                    onDeleteModel = {},
                    onSetSherpaNoiseScale = {},
                    onSetSherpaLengthScale = {},
                    onBack = {}
                )
            }
        }
    }
}
