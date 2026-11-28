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
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.SherpaVoiceRanges
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.models.VoiceGender
import com.mienaiknife.narra.domain.models.VoiceProfile
import com.mienaiknife.narra.domain.models.VoiceQuality
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.NarraProgressBar
import com.mienaiknife.narra.ui.components.SettingDropDownItem
import com.mienaiknife.narra.ui.components.SettingsActionRow
import com.mienaiknife.narra.ui.components.SettingsScaffold
import com.mienaiknife.narra.ui.components.SettingsSliderRow
import com.mienaiknife.narra.ui.components.flashHighlight
import com.mienaiknife.narra.ui.components.narraListItemColors
import com.mienaiknife.narra.ui.theme.LocalNarraSpacing
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.VoicesSettingsUiState
import com.mienaiknife.narra.ui.viewmodels.VoicesSettingsViewModel
import kotlin.math.roundToInt

@Composable
fun VoicesSettingsScreen(
    onBack: () -> Unit,
    onNavigateToPronunciations: () -> Unit,
    highlightSetting: String? = null,
    viewModel: VoicesSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    VoicesSettingsContent(
        uiState = uiState,
        highlightSetting = highlightSetting,
        onSetEngine = viewModel::setEngine,
        onSelectModel = viewModel::selectModel,
        onSetSpeakerId = viewModel::setSpeakerId,
        onDownloadModel = viewModel::downloadModel,
        onCancelDownload = viewModel::cancelDownload,
        onDeleteModel = viewModel::deleteModel,
        onSetVoiceNoiseScale = viewModel::setVoiceNoiseScale,
        onSetVoiceNoiseScaleW = viewModel::setVoiceNoiseScaleW,
        onSetVoiceLengthScale = viewModel::setVoiceLengthScale,
        onSetVoicePauseScale = viewModel::setVoicePauseScale,
        onSetVoiceExtraPause = viewModel::setVoiceExtraPause,
        onSetVoiceCpuThreads = viewModel::setVoiceCpuThreads,
        onResetVoiceParams = viewModel::resetVoiceParams,
        onNavigateToPronunciations = onNavigateToPronunciations,
        onBack = onBack,
    )
}

@Composable
fun VoicesSettingsContent(
    uiState: VoicesSettingsUiState,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
    onSetEngine: (String) -> Unit,
    onSelectModel: (String?) -> Unit,
    onSetSpeakerId: (Int) -> Unit,
    onDownloadModel: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onSetVoiceNoiseScale: (Float) -> Unit,
    onSetVoiceNoiseScaleW: (Float) -> Unit,
    onSetVoiceLengthScale: (Float) -> Unit,
    onSetVoicePauseScale: (Float) -> Unit,
    onSetVoiceExtraPause: (Int) -> Unit,
    onSetVoiceCpuThreads: (Int) -> Unit,
    onResetVoiceParams: () -> Unit,
    onNavigateToPronunciations: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    val engineSelectionRequester = remember { BringIntoViewRequester() }
    val androidTtsSettingsRequester = remember { BringIntoViewRequester() }
    val noiseScaleRequester = remember { BringIntoViewRequester() }
    val noiseScaleWRequester = remember { BringIntoViewRequester() }
    val lengthScaleRequester = remember { BringIntoViewRequester() }
    val pauseScaleRequester = remember { BringIntoViewRequester() }
    val extraPauseRequester = remember { BringIntoViewRequester() }
    val cpuThreadsRequester = remember { BringIntoViewRequester() }
    val voiceRequester = remember { BringIntoViewRequester() }
    val voiceDataRequester = remember { BringIntoViewRequester() }

    val engines = listOf(
        stringResource(R.string.settings_voices_android_engine_name),
        stringResource(R.string.settings_voices_ondevice_engine_name),
    )

    val curatedVoices = curatedVoicesFor(uiState.selectedModelId)
    val femaleSuffix = stringResource(R.string.reader_female_suffix)
    val maleSuffix = stringResource(R.string.reader_male_suffix)
    fun voiceLabelFor(voice: CuratedVoice): String = voice.name + when (voice.gender) {
        VoiceGender.FEMALE -> femaleSuffix
        VoiceGender.MALE -> maleSuffix
        VoiceGender.MIXED, VoiceGender.UNSPECIFIED -> ""
    }

    val availableLanguages = remember(uiState.availableModels) {
        uiState.availableModels
            .map { it.language.ifBlank { "und" } }
            .distinct()
            .sortedBy { languageDisplayName(it).lowercase() }
    }
    // A blank code means "all languages"; rememberSaveable cannot persist null.
    var languageFilterCode by rememberSaveable { mutableStateOf("") }
    val languageFilter = languageFilterCode.takeIf { it.isNotBlank() }
    LaunchedEffect(availableLanguages, languageFilterCode) {
        if (languageFilterCode.isNotBlank() && languageFilterCode !in availableLanguages) {
            languageFilterCode = ""
        }
    }

    val isInitializing = uiState.engineState is TtsState.Initializing
    val capabilities = uiState.voiceCapabilities
    val onDevice = uiState.selectedEngine == "ondevice"
    val hasModel = uiState.selectedModel != null

    LaunchedEffect(
        highlightSetting,
        onDevice,
        hasModel,
        capabilities,
        curatedVoices,
    ) {
        val fallback = if (onDevice) voiceDataRequester else engineSelectionRequester
        val target = when (highlightSetting) {
            "engineSelection" -> engineSelectionRequester
            "androidTtsSettings" -> if (onDevice) engineSelectionRequester else androidTtsSettingsRequester
            "voiceData" -> if (onDevice) voiceDataRequester else engineSelectionRequester
            "noiseScale" -> if (onDevice && hasModel && capabilities.noiseScale) noiseScaleRequester else fallback
            "noiseScaleW" -> if (onDevice && hasModel && capabilities.noiseScaleW) noiseScaleWRequester else fallback
            "lengthScale" -> if (onDevice && hasModel && capabilities.lengthScale) lengthScaleRequester else fallback
            "pauseScale" -> if (onDevice && hasModel && capabilities.pauseScale) pauseScaleRequester else fallback
            "extraPause" -> if (onDevice && hasModel) extraPauseRequester else fallback
            "cpuThreads" -> if (onDevice && hasModel) cpuThreadsRequester else fallback
            "voice" -> if (onDevice && curatedVoices.size > 1) voiceRequester else fallback
            else -> null
        }
        target?.bringIntoView()
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_voices_title),
        onBack = onBack,
        modifier = modifier,
        showInitProgress = isInitializing,
    ) {
        SettingDropDownItem(
            title = stringResource(R.string.settings_voices_engine_selection),
            subtitle = null,
            selectedValue = uiState.selectedEngine,
            options = listOf("android", "ondevice"),
            onValueChange = onSetEngine,
            optionLabel = { key -> if (key == "ondevice") engines[1] else engines[0] },
            enabled = !isInitializing,
            modifier = Modifier
                .bringIntoViewRequester(engineSelectionRequester)
                .flashHighlight(highlightSetting == "engineSelection"),
        )

        when (uiState.selectedEngine) {
            "android" -> {
                Text(
                    text = stringResource(R.string.settings_voices_android_settings),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(androidTtsSettingsRequester)
                        .flashHighlight(highlightSetting == "androidTtsSettings")
                        .padding(vertical = 16.dp)
                        .clickable(role = Role.Button) {
                            try {
                                context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                            } catch (_: Exception) {
                                // Fallback or handle error
                            }
                        },
                )
            }

            "ondevice" -> {
                VoiceSectionHeader(title = stringResource(R.string.settings_voices_voice_data)) {
                    if (availableLanguages.isNotEmpty()) {
                        LanguageFilter(
                            languages = availableLanguages,
                            selectedLanguage = languageFilter,
                            enabled = !isInitializing,
                            onSelect = { languageFilterCode = it ?: "" },
                        )
                    }
                }

                VoiceDataBox(
                    models = uiState.availableModels,
                    selectedModelId = uiState.selectedModelId,
                    languageFilter = languageFilter,
                    isInitializing = isInitializing,
                    highlightSetting = highlightSetting,
                    modifier = Modifier
                        .bringIntoViewRequester(voiceDataRequester)
                        .flashHighlight(highlightSetting == "voiceData"),
                    onSelectModel = onSelectModel,
                    onDownloadModel = onDownloadModel,
                    onCancelDownload = onCancelDownload,
                    onDeleteModel = onDeleteModel,
                )

                val selectedModel = uiState.selectedModel
                if (selectedModel != null) {
                    VoiceSectionHeader(title = stringResource(R.string.settings_voices_voice_settings)) {
                        if (uiState.hasCustomVoiceParams) {
                            TextButton(
                                onClick = onResetVoiceParams,
                                enabled = !isInitializing,
                                contentPadding = PaddingValues(horizontal = 0.dp),
                            ) {
                                Text(stringResource(R.string.settings_voices_reset))
                            }
                        }
                    }

                    if (curatedVoices.size > 1) {
                        SettingDropDownItem(
                            title = stringResource(R.string.settings_voices_voice),
                            subtitle = null,
                            selectedValue = curatedVoices.firstOrNull { it.id == uiState.voiceParams.speakerId } ?: curatedVoices.first(),
                            options = curatedVoices,
                            onValueChange = { onSetSpeakerId(it.id) },
                            optionLabel = { voiceLabelFor(it) },
                            enabled = !isInitializing,
                            modifier = Modifier
                                .bringIntoViewRequester(voiceRequester)
                                .flashHighlight(highlightSetting == "voice"),
                        )
                    }

                    if (capabilities.noiseScale) {
                        SettingsSliderRow(
                            title = stringResource(
                                R.string.settings_voices_noise_scale,
                                uiState.voiceParams.noiseScale,
                            ),
                            value = uiState.voiceParams.noiseScale,
                            onValueChange = onSetVoiceNoiseScale,
                            valueRange = SherpaVoiceRanges.NOISE_SCALE,
                            enabled = !isInitializing,
                            modifier = Modifier
                                .bringIntoViewRequester(noiseScaleRequester)
                                .flashHighlight(highlightSetting == "noiseScale"),
                        )
                    }

                    if (capabilities.lengthScale) {
                        SettingsSliderRow(
                            title = stringResource(
                                R.string.settings_voices_length_scale,
                                uiState.voiceParams.lengthScale,
                            ),
                            value = uiState.voiceParams.lengthScale,
                            onValueChange = onSetVoiceLengthScale,
                            valueRange = SherpaVoiceRanges.LENGTH_SCALE,
                            enabled = !isInitializing,
                            modifier = Modifier
                                .bringIntoViewRequester(lengthScaleRequester)
                                .flashHighlight(highlightSetting == "lengthScale"),
                        )
                    }

                    SettingsSliderRow(
                        title = stringResource(
                            R.string.settings_voices_extra_pause,
                            uiState.voiceParams.extraPauseMs,
                        ),
                        value = uiState.voiceParams.extraPauseMs.toFloat(),
                        onValueChange = { onSetVoiceExtraPause((it / 50f).roundToInt() * 50) },
                        valueRange = 0f..1000f,
                        enabled = !isInitializing,
                        modifier = Modifier
                            .bringIntoViewRequester(extraPauseRequester)
                            .flashHighlight(highlightSetting == "extraPause"),
                    )

                    if (capabilities.noiseScaleW) {
                        SettingsSliderRow(
                            title = stringResource(
                                R.string.settings_voices_noise_scale_w,
                                uiState.voiceParams.noiseScaleW,
                            ),
                            value = uiState.voiceParams.noiseScaleW,
                            onValueChange = onSetVoiceNoiseScaleW,
                            valueRange = SherpaVoiceRanges.NOISE_SCALE_W,
                            enabled = !isInitializing,
                            modifier = Modifier
                                .bringIntoViewRequester(noiseScaleWRequester)
                                .flashHighlight(highlightSetting == "noiseScaleW"),
                        )
                    }

                    if (capabilities.pauseScale) {
                        SettingsSliderRow(
                            title = stringResource(
                                R.string.settings_voices_pause_scale,
                                uiState.voiceParams.pauseScale,
                            ),
                            value = uiState.voiceParams.pauseScale,
                            onValueChange = onSetVoicePauseScale,
                            valueRange = SherpaVoiceRanges.PAUSE_SCALE,
                            enabled = !isInitializing,
                            modifier = Modifier
                                .bringIntoViewRequester(pauseScaleRequester)
                                .flashHighlight(highlightSetting == "pauseScale"),
                        )
                    }

                    SettingsSliderRow(
                        title = stringResource(
                            R.string.settings_voices_cpu_threads,
                            uiState.voiceParams.cpuThreads,
                        ),
                        value = uiState.voiceParams.cpuThreads.toFloat(),
                        onValueChange = { onSetVoiceCpuThreads(it.roundToInt()) },
                        valueRange = 1f..8f,
                        steps = 6,
                        enabled = !isInitializing,
                        modifier = Modifier
                            .bringIntoViewRequester(cpuThreadsRequester)
                            .flashHighlight(highlightSetting == "cpuThreads"),
                    )
                }

                SettingsActionRow(
                    title = stringResource(R.string.settings_voices_pronunciations),
                    subtitle = stringResource(R.string.settings_voices_pronunciations_desc),
                    onClick = onNavigateToPronunciations,
                )
            }
        }
    }
}

/**
 * A settings group heading with an optional trailing control (the language filter, the reset
 * button). The trailing control is a 48 dp touch target, so the heading and the control share a
 * centred 48 dp row instead of padding the heading text: padding the text would leave the control
 * sitting above the heading's line.
 */
@Composable
private fun VoiceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        // The group gap belongs to the row so the heading keeps its spacing whether or not a
        // trailing control is present.
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun VoiceDataBox(
    models: List<TtsModel>,
    selectedModelId: String?,
    languageFilter: String?,
    isInitializing: Boolean,
    highlightSetting: String?,
    modifier: Modifier = Modifier,
    onSelectModel: (String?) -> Unit,
    onDownloadModel: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val visibleModels = remember(models, languageFilter) {
        filterModelsByLanguage(models, languageFilter)
    }
    val rows = remember(visibleModels) {
        groupModelsByLanguage(visibleModels) { languageDisplayName(it) }.flatMap { it.models }
    }
    val modelsLoaded = rows.isNotEmpty()

    LaunchedEffect(selectedModelId, languageFilter, modelsLoaded) {
        if (!modelsLoaded) return@LaunchedEffect
        val selectedIndex = rows.indexOfFirst { it.id == selectedModelId }
        // A filter change should start at the top unless the selected voice is still visible.
        listState.scrollToItem(if (selectedIndex >= 0) selectedIndex else 0)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (!modelsLoaded) {
            Text(
                text = stringResource(R.string.settings_voices_no_models),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = LocalNarraSpacing.current.voiceListMaxHeight),
            ) {
                itemsIndexed(
                    items = rows,
                    key = { _, model -> model.id },
                ) { index, model ->
                    TtsModelItem(
                        model = model,
                        isSelected = selectedModelId == model.id,
                        onSelect = { if (!isInitializing) onSelectModel(model.id) },
                        onDownload = { onDownloadModel(model.id) },
                        onCancelDownload = { onCancelDownload(model.id) },
                        onDelete = { onDeleteModel(model.id) },
                        containerColor = Color.Transparent,
                        enabled = !isInitializing,
                    )
                    if (index < rows.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageFilter(
    languages: List<String>,
    selectedLanguage: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            enabled = enabled,
            // Align the label and caret with the 16dp content gutter, like Playback's values.
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            Text(
                text = selectedLanguage?.let(::languageDisplayName)
                    ?: stringResource(R.string.settings_voices_all_languages),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_voices_all_languages)) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
                trailingIcon = {
                    if (selectedLanguage == null) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                },
            )
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(languageDisplayName(language)) },
                    onClick = {
                        onSelect(language)
                        expanded = false
                    },
                    trailingIcon = {
                        if (selectedLanguage == language) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                )
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
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true,
) {
    val isDownloading = model.progress > 0f && !model.isDownloaded && model.lastError == null

    ListItem(
        modifier = Modifier
            .selectable(
                selected = isSelected,
                enabled = model.isDownloaded && enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
        colors = narraListItemColors(containerColor),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (model.isDownloaded || isDownloading) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings_voices_selected_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        supportingContent = {
            val description = voiceProfileDescription(model.voice)
            Column(
                modifier = Modifier.padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical),
            ) {
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val stageLabel = when {
                        model.progress < 0.40f -> stringResource(R.string.settings_voices_downloading)
                        model.progress < 0.85f -> stringResource(R.string.settings_voices_extracting)
                        else -> stringResource(R.string.settings_voices_finalizing)
                    }
                    Text(
                        text = stageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NarraProgressBar(
                        progress = model.progress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (model.lastError != null && !isDownloading) {
                    Text(
                        text = stringResource(R.string.settings_voices_error, model.lastError),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        trailingContent = {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    model.isDownloaded -> {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.settings_voices_delete_model_desc),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    isDownloading -> {
                        IconButton(onClick = onCancelDownload) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.settings_voices_cancel_download_desc),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    else -> {
                        IconButton(onClick = onDownload) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(R.string.settings_voices_download_model_desc),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
fun VoicesSettingsScreenPreview() {
    val navController = rememberNavController()
    val mockUiState = VoicesSettingsUiState(
        availableModels = listOf(
            TtsModel(
                id = "vits-piper-en_US-amy-low",
                name = "Piper Amy",
                language = "en",
                voice = VoiceProfile(
                    quality = VoiceQuality.LOW,
                    gender = VoiceGender.FEMALE,
                    locale = "en-US",
                ),
                type = TtsModelType.VITS,
                modelUrl = "",
                tokensUrl = "",
                isDownloaded = true,
            ),
            TtsModel(
                id = "vits-piper-en_US-ryan-medium",
                name = "Piper Ryan",
                language = "en",
                voice = VoiceProfile(
                    quality = VoiceQuality.MEDIUM,
                    gender = VoiceGender.MALE,
                    locale = "en-US",
                ),
                type = TtsModelType.VITS,
                modelUrl = "",
                tokensUrl = "",
                isDownloaded = false,
            ),
            TtsModel(
                id = "matcha-en-ljspeech",
                name = "Matcha Icefall",
                language = "en",
                voice = VoiceProfile(
                    quality = VoiceQuality.HIGH,
                    gender = VoiceGender.FEMALE,
                    locale = "en-US",
                ),
                type = TtsModelType.MATCHA,
                modelUrl = "",
                tokensUrl = "",
                isDownloaded = false,
                progress = 0.5f,
            ),
        ),
        selectedEngine = "ondevice",
        selectedModelId = "vits-piper-en_US-amy-low",
    )

    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                VoicesSettingsContent(
                    uiState = mockUiState,
                    highlightSetting = null,
                    onSetEngine = {},
                    onSelectModel = {},
                    onSetSpeakerId = {},
                    onDownloadModel = {},
                    onCancelDownload = {},
                    onDeleteModel = {},
                    onSetVoiceNoiseScale = {},
                    onSetVoiceNoiseScaleW = {},
                    onSetVoiceLengthScale = {},
                    onSetVoicePauseScale = {},
                    onSetVoiceExtraPause = {},
                    onSetVoiceCpuThreads = {},
                    onResetVoiceParams = {},
                    onNavigateToPronunciations = {},
                    onBack = {},
                )
            }
        }
    }
}
