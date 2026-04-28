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
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mienaiknife.narra.data.local.AppDatabase
import com.mienaiknife.narra.data.settings.SyncSettingsManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

@HiltWorker
class DatabaseExportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val appDatabase: AppDatabase,
    private val syncSettingsManager: SyncSettingsManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriString = inputData.getString("uri") ?: return@withContext Result.failure()
        val uri = Uri.parse(uriString)

        try {
            // 1. Checkpoint to make the main file consistent
            // Use try-catch because if the DB is closed or there's an issue, we don't want to crash
            try {
                appDatabase.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseExportWorker", "Checkpoint failed", e)
                // Continue anyway, it might still be a valid snapshot
            }

            // 2. Get DB file path
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (!dbFile.exists()) {
                android.util.Log.e("DatabaseExportWorker", "Database file not found")
                return@withContext Result.failure()
            }

            // 3. Write to URI
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure()

            syncSettingsManager.updateLastExportTimestamp()
            android.util.Log.i("DatabaseExportWorker", "Database auto-export successful")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("DatabaseExportWorker", "Database auto-export failed", e)
            Result.retry()
        }
    }
}
