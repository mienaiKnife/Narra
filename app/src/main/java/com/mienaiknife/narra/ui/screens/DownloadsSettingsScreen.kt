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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.ScreenHeader
import com.mienaiknife.narra.ui.components.SettingDropDownItem
import com.mienaiknife.narra.ui.components.SettingsActionRow
import com.mienaiknife.narra.ui.components.SettingsSwitchRow
import com.mienaiknife.narra.ui.components.flashHighlight
import com.mienaiknife.narra.ui.theme.LocalNarraSpacing
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.DownloadsSettingsUiState
import com.mienaiknife.narra.ui.viewmodels.DownloadsSettingsViewModel
import com.mienaiknife.narra.utils.DateUtils

@Composable
fun DownloadsSettingsScreen(
    onBack: () -> Unit,
    highlightSetting: String? = null,
    viewModel: DownloadsSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            viewModel.importOpml(inputStream)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml"),
    ) { uri ->
        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
            viewModel.exportOpml(outputStream)
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
            viewModel.backupDatabase(outputStream)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            viewModel.restoreDatabase(inputStream)
        }
    }

    val autoExportLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (e: Exception) {
                android.util.Log.e("DownloadsSettingsScreen", "Failed to take persistable permission", e)
            }
            viewModel.setAutoExportUri(it.toString())
            viewModel.setAutoExportEnabled(true)
        }
    }

    val showDeleteConfirm = remember { mutableStateOf(false) }

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            title = { Text(stringResource(R.string.settings_downloads_delete_db)) },
            text = { Text(stringResource(R.string.settings_downloads_delete_db_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDatabase()
                        showDeleteConfirm.value = false
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    DownloadsSettingsContent(
        uiState = uiState,
        highlightSetting = highlightSetting,
        onDownloadOverWifiOnlyChange = { viewModel.setDownloadOverWifiOnly(it) },
        onRefreshIntervalChange = { viewModel.setRefreshInterval(it) },
        onInboxInitialLimitChange = { viewModel.setInboxInitialLimit(it) },
        onImportOpml = { importLauncher.launch(arrayOf("application/xml", "text/xml", "application/octet-stream", "*/*")) },
        onExportOpml = { exportLauncher.launch("narra-subscriptions.opml") },
        onBackupDatabase = { backupLauncher.launch("narra-backup.db") },
        onRestoreDatabase = { restoreLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
        onAutoExportEnabledChange = { enabled ->
            if (enabled && uiState.autoExportUri == null) {
                autoExportLocationLauncher.launch("narra_db.sqlite")
            } else {
                viewModel.setAutoExportEnabled(enabled)
            }
        },
        onAutoImportEnabledChange = { viewModel.setAutoImportEnabled(it) },
        onSetAutoExportLocation = { autoExportLocationLauncher.launch("narra_db.sqlite") },
        onDeleteDatabase = { showDeleteConfirm.value = true },
        onBack = onBack,
    )
}

@Composable
fun DownloadsSettingsContent(
    uiState: DownloadsSettingsUiState,
    highlightSetting: String? = null,
    onDownloadOverWifiOnlyChange: (Boolean) -> Unit,
    onRefreshIntervalChange: (String) -> Unit,
    onInboxInitialLimitChange: (String) -> Unit,
    onImportOpml: () -> Unit,
    onExportOpml: () -> Unit,
    onBackupDatabase: () -> Unit,
    onRestoreDatabase: () -> Unit,
    onAutoExportEnabledChange: (Boolean) -> Unit,
    onAutoImportEnabledChange: (Boolean) -> Unit,
    onSetAutoExportLocation: () -> Unit,
    onDeleteDatabase: () -> Unit,
    onBack: () -> Unit,
) {
    val downloadOverWifiOnlyRequester = remember { BringIntoViewRequester() }
    val refreshIntervalRequester = remember { BringIntoViewRequester() }
    val inboxInitialLimitRequester = remember { BringIntoViewRequester() }
    val backupDatabaseRequester = remember { BringIntoViewRequester() }
    val restoreDatabaseRequester = remember { BringIntoViewRequester() }
    val autoExportEnabledRequester = remember { BringIntoViewRequester() }
    val autoImportEnabledRequester = remember { BringIntoViewRequester() }
    val autoExportLocationRequester = remember { BringIntoViewRequester() }
    val deleteDatabaseRequester = remember { BringIntoViewRequester() }
    val importFeedsRequester = remember { BringIntoViewRequester() }
    val exportFeedsRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(highlightSetting) {
        when (highlightSetting) {
            "downloadOverWifiOnly" -> downloadOverWifiOnlyRequester.bringIntoView()
            "refreshInterval" -> refreshIntervalRequester.bringIntoView()
            "inboxInitialLimit" -> inboxInitialLimitRequester.bringIntoView()
            "exportDatabase" -> backupDatabaseRequester.bringIntoView()
            "importDatabase" -> restoreDatabaseRequester.bringIntoView()
            "autoExportDatabase" -> autoExportEnabledRequester.bringIntoView()
            "autoImportDatabase" -> autoImportEnabledRequester.bringIntoView()
            "autoExportLocation" -> autoExportLocationRequester.bringIntoView()
            "deleteDatabase" -> deleteDatabaseRequester.bringIntoView()
            "importFeeds" -> importFeedsRequester.bringIntoView()
            "exportFeeds" -> exportFeedsRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ScreenHeader(
            title = stringResource(R.string.settings_downloads_title),
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_downloads_network_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(downloadOverWifiOnlyRequester)
                    .flashHighlight(highlightSetting == "downloadOverWifiOnly"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical),
                ) {
                    Text(
                        text = stringResource(R.string.settings_downloads_wifi_only),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_downloads_wifi_only_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.downloadOverWifiOnly,
                    onCheckedChange = onDownloadOverWifiOnlyChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }

            Text(
                text = stringResource(R.string.settings_downloads_automation_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            SettingDropDownItem(
                title = stringResource(R.string.settings_downloads_refresh_inbox),
                subtitle = stringResource(R.string.settings_downloads_refresh_inbox_desc),
                selectedValue = uiState.refreshInterval,
                options = listOf("Never", "1 hour", "3 hours", "6 hours", "12 hours", "24 hours"),
                onValueChange = onRefreshIntervalChange,
                optionLabel = { refreshIntervalLabel(it) },
                modifier = Modifier
                    .bringIntoViewRequester(refreshIntervalRequester)
                    .flashHighlight(highlightSetting == "refreshInterval"),
            )

            SettingDropDownItem(
                title = stringResource(R.string.settings_downloads_inbox_limit),
                subtitle = stringResource(R.string.settings_downloads_inbox_limit_desc),
                selectedValue = uiState.inboxInitialLimit,
                options = listOf("1", "5", "10", "20", "50", "All"),
                onValueChange = onInboxInitialLimitChange,
                optionLabel = { if (it == "All") stringResource(R.string.settings_downloads_inbox_limit_all) else it },
                modifier = Modifier
                    .bringIntoViewRequester(inboxInitialLimitRequester)
                    .flashHighlight(highlightSetting == "inboxInitialLimit"),
            )

            Text(
                text = stringResource(R.string.settings_downloads_opml_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            SettingsActionRow(
                title = stringResource(R.string.settings_downloads_import_feeds),
                subtitle = stringResource(R.string.settings_downloads_import_feeds_desc),
                onClick = onImportOpml,
                modifier = Modifier
                    .bringIntoViewRequester(importFeedsRequester)
                    .flashHighlight(highlightSetting == "importFeeds"),
            )

            SettingsActionRow(
                title = stringResource(R.string.settings_downloads_export_feeds),
                subtitle = stringResource(R.string.settings_downloads_export_feeds_desc),
                onClick = onExportOpml,
                modifier = Modifier
                    .bringIntoViewRequester(exportFeedsRequester)
                    .flashHighlight(highlightSetting == "exportFeeds"),
            )

            Text(
                text = stringResource(R.string.settings_downloads_database_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            SettingsActionRow(
                title = stringResource(R.string.settings_downloads_export_db),
                subtitle = stringResource(R.string.settings_downloads_export_db_desc),
                onClick = onBackupDatabase,
                verticalPadding = 12.dp,
                modifier = Modifier
                    .bringIntoViewRequester(backupDatabaseRequester)
                    .flashHighlight(highlightSetting == "exportDatabase"),
            ) {
                if (uiState.lastExportTimestamp > 0) {
                    Text(
                        text = stringResource(R.string.settings_downloads_last_export, DateUtils.formatDateTime(uiState.lastExportTimestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            SettingsActionRow(
                title = stringResource(R.string.settings_downloads_import_db),
                subtitle = stringResource(R.string.settings_downloads_import_db_desc),
                onClick = onRestoreDatabase,
                modifier = Modifier
                    .bringIntoViewRequester(restoreDatabaseRequester)
                    .flashHighlight(highlightSetting == "importDatabase"),
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_downloads_auto_export),
                subtitle = stringResource(R.string.settings_downloads_auto_export_desc),
                checked = uiState.autoExportEnabled,
                onCheckedChange = onAutoExportEnabledChange,
                verticalPadding = 12.dp,
                modifier = Modifier
                    .bringIntoViewRequester(autoExportEnabledRequester)
                    .flashHighlight(highlightSetting == "autoExportDatabase"),
            )

            if (uiState.autoExportEnabled) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_downloads_auto_import),
                    subtitle = stringResource(R.string.settings_downloads_auto_import_desc),
                    checked = uiState.autoImportEnabled,
                    onCheckedChange = onAutoImportEnabledChange,
                    enabled = uiState.autoExportUri != null,
                    verticalPadding = 12.dp,
                    modifier = Modifier
                        .bringIntoViewRequester(autoImportEnabledRequester)
                        .flashHighlight(highlightSetting == "autoImportDatabase"),
                )

                SettingsActionRow(
                    title = stringResource(R.string.settings_downloads_auto_export_location),
                    subtitle = if (uiState.autoExportUri != null) {
                        stringResource(R.string.settings_downloads_auto_export_location_set)
                    } else {
                        stringResource(R.string.settings_downloads_auto_export_location_not_set)
                    },
                    onClick = onSetAutoExportLocation,
                    modifier = Modifier
                        .bringIntoViewRequester(autoExportLocationRequester)
                        .flashHighlight(highlightSetting == "autoExportLocation"),
                )

                if (uiState.lastExportTimestamp > 0 && uiState.autoExportEnabled) {
                    Text(
                        text = stringResource(R.string.settings_downloads_last_export, DateUtils.formatDateTime(uiState.lastExportTimestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (uiState.pendingImport) {
                    Text(
                        text = stringResource(R.string.settings_downloads_staged_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            SettingsActionRow(
                title = stringResource(R.string.settings_downloads_delete_db),
                subtitle = stringResource(R.string.settings_downloads_delete_db_desc),
                onClick = onDeleteDatabase,
                titleColor = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .bringIntoViewRequester(deleteDatabaseRequester)
                    .flashHighlight(highlightSetting == "deleteDatabase"),
            )
        }
    }
}

@Composable
private fun refreshIntervalLabel(storedValue: String): String = when (storedValue) {
    "Never" -> stringResource(R.string.settings_downloads_refresh_never)
    "1 hour" -> stringResource(R.string.unit_1_hour)
    "3 hours" -> pluralStringResource(R.plurals.unit_hours, 3, 3)
    "6 hours" -> pluralStringResource(R.plurals.unit_hours, 6, 6)
    "12 hours" -> pluralStringResource(R.plurals.unit_hours, 12, 12)
    "24 hours" -> pluralStringResource(R.plurals.unit_hours, 24, 24)
    else -> storedValue
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DownloadsSettingsScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                DownloadsSettingsContent(
                    uiState = DownloadsSettingsUiState(
                        autoExportEnabled = true,
                        autoImportEnabled = true,
                        autoExportUri = "content://com.android.externalstorage.documents/document/primary%3ANarra%2Fnarra_db.sqlite",
                        lastExportTimestamp = System.currentTimeMillis(),
                    ),
                    highlightSetting = null,
                    onDownloadOverWifiOnlyChange = {},
                    onRefreshIntervalChange = {},
                    onInboxInitialLimitChange = {},
                    onImportOpml = {},
                    onExportOpml = {},
                    onBackupDatabase = {},
                    onRestoreDatabase = {},
                    onAutoExportEnabledChange = {},
                    onAutoImportEnabledChange = {},
                    onSetAutoExportLocation = {},
                    onDeleteDatabase = {},
                    onBack = {},
                )
            }
        }
    }
}
