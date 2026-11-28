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

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.CustomPronunciation
import com.mienaiknife.narra.domain.pronunciation.PronunciationValidator
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.NarraListItem
import com.mienaiknife.narra.ui.components.NarraTextFieldShape
import com.mienaiknife.narra.ui.components.SettingsScaffold
import com.mienaiknife.narra.ui.components.SettingsSectionLabel
import com.mienaiknife.narra.ui.components.narraTextFieldColors
import com.mienaiknife.narra.ui.theme.LocalNarraSpacing
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.PronunciationsUiState
import com.mienaiknife.narra.ui.viewmodels.PronunciationsViewModel
import com.mienaiknife.narra.ui.viewmodels.toUiText

/** Validation errors that belong to the word field rather than the ARPAbet field. */
private val WORD_VALIDATION_ERRORS = setOf(
    PronunciationValidator.Error.EMPTY_WORD,
    PronunciationValidator.Error.WORD_TOO_LONG,
    PronunciationValidator.Error.INVALID_WORD_CHARACTER,
)

@Composable
fun PronunciationsSettingsScreen(
    onBack: () -> Unit,
    viewModel: PronunciationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    PronunciationsSettingsContent(
        uiState = uiState,
        onBack = onBack,
        onSave = { word, arpabet, original -> viewModel.save(word, arpabet, original) },
        onDelete = viewModel::delete,
        onPreview = viewModel::preview,
    )
}

@Composable
fun PronunciationsSettingsContent(
    uiState: PronunciationsUiState,
    onBack: () -> Unit,
    onSave: (String, String, CustomPronunciation?) -> Unit,
    onDelete: (CustomPronunciation) -> Unit,
    onPreview: (CustomPronunciation) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editorEntry by remember { mutableStateOf<CustomPronunciation?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<CustomPronunciation?>(null) }

    if (showEditor) {
        PronunciationEditorDialog(
            initialEntry = editorEntry,
            onDismiss = { showEditor = false },
            onSave = { word, arpabet ->
                onSave(word, arpabet, editorEntry)
                showEditor = false
            },
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text(stringResource(R.string.settings_pronunciations_delete_title)) },
            text = {
                Text(
                    stringResource(R.string.settings_pronunciations_delete_message, entry.word),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(entry)
                        entryToDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_pronunciations_title),
        onBack = onBack,
        modifier = modifier,
        showInitProgress = uiState.engineState is TtsState.Initializing,
    ) {
        Text(
            text = stringResource(R.string.settings_pronunciations_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                editorEntry = null
                showEditor = true
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_pronunciations_add))
        }

        SettingsSectionLabel(
            text = stringResource(R.string.settings_pronunciations_saved_section),
        )

        if (uiState.entries.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_pronunciations_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    uiState.entries.forEachIndexed { index, entry ->
                        PronunciationListItem(
                            entry = entry,
                            previewEnabled = uiState.previewEnabled,
                            onPreview = { onPreview(entry) },
                            onEdit = {
                                editorEntry = entry
                                showEditor = true
                            },
                            onDelete = { entryToDelete = entry },
                        )
                        if (index < uiState.entries.lastIndex) {
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
}

@Composable
private fun PronunciationListItem(
    entry: CustomPronunciation,
    previewEnabled: Boolean,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val ipa = remember(entry.word, entry.arpabet) {
        when (val validated = PronunciationValidator.validate(entry.word, entry.arpabet)) {
            is PronunciationValidator.Result.Valid -> validated.ipa
            is PronunciationValidator.Result.Invalid -> null
        }
    }
    NarraListItem(
        title = entry.word,
        onClick = onEdit,
        supportingContent = {
            Column(
                modifier = Modifier.padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical),
            ) {
                Text(
                    text = entry.arpabet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (ipa != null) {
                    Text(
                        text = stringResource(R.string.settings_pronunciations_ipa_preview, ipa),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPreview,
                    enabled = previewEnabled,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.settings_pronunciations_play_desc),
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more_options),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ValidationMessage(
    error: PronunciationValidator.Error,
    detail: String?,
) {
    Text(
        text = error.toUiText(detail).asString(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
fun PronunciationEditorDialog(
    initialEntry: CustomPronunciation?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var word by remember(initialEntry) { mutableStateOf(initialEntry?.word.orEmpty()) }
    var arpabet by remember(initialEntry) { mutableStateOf(initialEntry?.arpabet.orEmpty()) }
    var showHelp by remember { mutableStateOf(false) }

    val validation = remember(word, arpabet) { PronunciationValidator.validate(word, arpabet) }
    val ipa = (validation as? PronunciationValidator.Result.Valid)?.ipa
    val canSave = validation is PronunciationValidator.Result.Valid
    val invalid = validation as? PronunciationValidator.Result.Invalid
    // Do not nag about a missing pronunciation until the user has started typing one.
    val shownError = invalid?.takeIf {
        word.isNotBlank() &&
            (it.error != PronunciationValidator.Error.EMPTY_PRONUNCIATION || arpabet.isNotBlank())
    }
    val wordError = shownError?.takeIf { it.error in WORD_VALIDATION_ERRORS }
    val arpabetError = shownError?.takeIf { it.error !in WORD_VALIDATION_ERRORS }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                stringResource(
                    if (initialEntry == null) {
                        R.string.settings_pronunciations_add
                    } else {
                        R.string.settings_pronunciations_edit
                    },
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text(stringResource(R.string.settings_pronunciations_word)) },
                    singleLine = true,
                    isError = wordError != null,
                    shape = NarraTextFieldShape,
                    colors = narraTextFieldColors(),
                    supportingText = if (wordError != null) {
                        { ValidationMessage(wordError.error, wordError.detail) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = arpabet,
                    onValueChange = { arpabet = it },
                    label = { Text(stringResource(R.string.settings_pronunciations_arpabet)) },
                    placeholder = { Text(stringResource(R.string.settings_pronunciations_arpabet_placeholder)) },
                    singleLine = true,
                    isError = arpabetError != null,
                    shape = NarraTextFieldShape,
                    colors = narraTextFieldColors(),
                    supportingText = if (arpabetError != null) {
                        { ValidationMessage(arpabetError.error, arpabetError.detail) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (ipa != null) {
                    Text(
                        text = stringResource(R.string.settings_pronunciations_ipa_preview, ipa),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                TextButton(
                    onClick = { showHelp = true },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(stringResource(R.string.settings_pronunciations_help))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(word, arpabet) },
                enabled = canSave,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )

    if (showHelp) {
        ArpabetHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun ArpabetHelpDialog(onDismiss: () -> Unit) {
    val reference = stringArrayResource(R.array.settings_pronunciations_arpabet_help).joinToString("\n")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(stringResource(R.string.settings_pronunciations_help_title)) },
        text = {
            Text(
                text = reference,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@PreviewLightDark
@Composable
fun PronunciationsSettingsScreenPreview() {
    val navController = rememberNavController()
    val entries = listOf(
        CustomPronunciation(word = "Narra", arpabet = "N AA1 R AH0"),
        CustomPronunciation(word = "Sherpa", arpabet = "SH ER1 P AH0"),
        CustomPronunciation(word = "Kokoro", arpabet = "K OW0 K AO1 R OW0"),
    )

    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                PronunciationsSettingsContent(
                    uiState = PronunciationsUiState(
                        entries = entries,
                        engineState = TtsState.Ready,
                    ),
                    onBack = {},
                    onSave = { _, _, _ -> },
                    onDelete = {},
                    onPreview = {},
                )
            }
        }
    }
}
