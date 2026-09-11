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
package com.mienaiknife.narra.service

import androidx.room.InvalidationTracker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mienaiknife.narra.data.local.AppDatabase
import com.mienaiknife.narra.data.local.backup.STAGED_BACKUP_FILE
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.data.settings.SyncSettingsManager
import com.mienaiknife.narra.data.workers.DatabaseExportWorker
import com.mienaiknife.narra.data.workers.DatabaseImportWorker
import com.mienaiknife.narra.data.workers.FeedRefreshWorker
import com.mienaiknife.narra.domain.repository.ImportExportRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager
@Inject
constructor(
    private val appDatabase: AppDatabase,
    private val syncSettingsManager: SyncSettingsManager,
    private val downloadSettingsManager: DownloadSettingsManager,
    private val importExportRepository: ImportExportRepository,
    private val workManager: WorkManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val exportTrigger = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    private var isStarted = false

    init {
        scope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            exportTrigger
                .debounce(30000) // 30 seconds debounce to avoid excessive exports
                .collect { uri ->
                    enqueueExport(uri)
                }
        }
    }

    @Synchronized
    fun start() {
        if (isStarted) return
        isStarted = true

        scope.launch {
            combine(
                syncSettingsManager.autoExportEnabled,
                syncSettingsManager.autoImportEnabled,
                syncSettingsManager.autoExportUri,
            ) { export, import, uri ->
                Triple(export, import, uri)
            }.collectLatest { (export, import, uri) ->
                if (uri != null) {
                    if (export) {
                        launch { observeChanges(uri) }
                    }
                    if (import) {
                        scheduleImportCheck()
                    } else {
                        cancelImportCheck()
                    }
                }
            }
        }

        scope.launch {
            combine(
                downloadSettingsManager.refreshInterval,
                downloadSettingsManager.downloadOverWifiOnly,
            ) { interval, wifiOnly ->
                interval to wifiOnly
            }.collectLatest { (interval, wifiOnly) ->
                scheduleFeedRefresh(interval, wifiOnly)
            }
        }
    }

    /**
     * Checks if a staged database exists and applies it.
     * This MUST be called before the database is used, and must not run on the main thread.
     */
    suspend fun applyStagedDatabaseIfNecessary(context: android.content.Context) {
        if (syncSettingsManager.pendingImport.first()) {
            val stagedFile = context.getDatabasePath(STAGED_BACKUP_FILE)
            if (stagedFile.exists()) {
                try {
                    val result = FileInputStream(stagedFile).use { importExportRepository.restoreDatabase(it) }
                    if (result.isSuccess) {
                        stagedFile.delete()
                        syncSettingsManager.setPendingImport(false)
                        android.util.Log.i("SyncManager", "Staged backup applied successfully")
                    } else {
                        android.util.Log.e("SyncManager", "Failed to apply staged backup", result.exceptionOrNull())
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SyncManager", "Failed to apply staged backup", e)
                }
            }
        }
    }

    private suspend fun observeChanges(uriString: String) {
        val observer =
            object : InvalidationTracker.Observer(arrayOf("articles", "feeds")) {
                override fun onInvalidated(tables: Set<String>) {
                    exportTrigger.tryEmit(uriString)
                }
            }

        appDatabase.invalidationTracker.addObserver(observer)

        try {
            // Keep observing as long as this collectLatest block is active
            awaitCancellation()
        } finally {
            appDatabase.invalidationTracker.removeObserver(observer)
        }
    }

    private fun enqueueExport(uri: String) {
        val exportRequest =
            OneTimeWorkRequestBuilder<DatabaseExportWorker>()
                .setInputData(workDataOf("uri" to uri))
                .build()

        // APPEND_OR_REPLACE: let an in-flight export finish instead of cancelling it, then run
        // this one so the latest snapshot is still exported.
        workManager.enqueueUniqueWork(
            "database_auto_export",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            exportRequest,
        )
    }

    private fun scheduleImportCheck() {
        val importRequest =
            PeriodicWorkRequestBuilder<DatabaseImportWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()

        workManager.enqueueUniquePeriodicWork(
            "database_auto_import",
            ExistingPeriodicWorkPolicy.KEEP,
            importRequest,
        )
    }

    private fun cancelImportCheck() {
        workManager.cancelUniqueWork("database_auto_import")
    }

    private fun scheduleFeedRefresh(
        interval: String,
        wifiOnly: Boolean,
    ) {
        if (interval == "Never") {
            workManager.cancelUniqueWork("feed_refresh")
            return
        }

        val intervalMinutes =
            when (interval) {
                "1 hour" -> 60L
                "3 hours" -> 180L
                "6 hours" -> 360L
                "12 hours" -> 720L
                "24 hours" -> 1440L
                else -> 720L // Default to 12 hours
            }

        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()

        val refreshRequest =
            PeriodicWorkRequestBuilder<FeedRefreshWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

        workManager.enqueueUniquePeriodicWork(
            "feed_refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            refreshRequest,
        )
    }
}
