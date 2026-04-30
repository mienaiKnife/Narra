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
import android.provider.DocumentsContract
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mienaiknife.narra.data.settings.SyncSettingsManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

@HiltWorker
class DatabaseImportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val syncSettingsManager: SyncSettingsManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!syncSettingsManager.autoImportEnabled.first()) return@withContext Result.success()

        val uriString = syncSettingsManager.autoExportUri.first() ?: return@withContext Result.success()
        val uri = uriString.toUri()

        try {
            val lastModified = getRemoteModifiedTime(uri) ?: return@withContext Result.failure()
            val localLastKnown = syncSettingsManager.remoteLastModified.first()

            // If the remote file is newer than what we last staged
            if (lastModified > localLastKnown + 5000) { // 5s buffer
                android.util.Log.i("DatabaseImportWorker", "Newer database detected. Staging for import.")
                
                val stagedFile = context.getDatabasePath("narra_db_staged")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(stagedFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure()

                syncSettingsManager.setRemoteLastModified(lastModified)
                syncSettingsManager.setPendingImport(true)
                
                android.util.Log.i("DatabaseImportWorker", "Database successfully staged.")
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("DatabaseImportWorker", "Database auto-import check failed", e)
            Result.retry()
        }
    }

    private fun getRemoteModifiedTime(uri: android.net.Uri): Long? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    if (index != -1) cursor.getLong(index) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
