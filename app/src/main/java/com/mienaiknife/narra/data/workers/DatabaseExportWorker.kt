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
package com.mienaiknife.narra.data.workers

import android.content.Context
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mienaiknife.narra.data.settings.SyncSettingsManager
import com.mienaiknife.narra.domain.repository.ImportExportRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class DatabaseExportWorker
@AssistedInject
constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val importExportRepository: ImportExportRepository,
    private val syncSettingsManager: SyncSettingsManager,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriString = inputData.getString("uri") ?: return@withContext Result.failure()
        val uri = uriString.toUri()

        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                importExportRepository.backupDatabase(output).getOrThrow()
            } ?: return@withContext Result.failure()

            syncSettingsManager.updateLastExportTimestamp()
            android.util.Log.i("DatabaseExportWorker", "Database auto-export successful")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("DatabaseExportWorker", "Database auto-export failed", e)
            if (e.isRetryable() && runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        }
    }
}
