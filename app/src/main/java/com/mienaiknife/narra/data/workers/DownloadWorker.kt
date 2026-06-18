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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.mienaiknife.narra.domain.repository.ModelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class DownloadWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val modelRepository: ModelRepository,
) : CoroutineWorker(context, params) {
    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "download_channel"
    }

    override suspend fun doWork(): Result {
        val modelId = inputData.getString("modelId") ?: return Result.failure()

        // Use setForeground to make it a foreground service
        try {
            setForeground(createForegroundInfo(modelId))
        } catch (e: Exception) {
            android.util.Log.e("DownloadWorker", "Failed to set foreground", e)
            // Continue anyway, it might still work as a background worker
        }

        return try {
            val result = modelRepository.downloadModel(modelId)
            if (result.isSuccess) {
                Result.success()
            } else {
                android.util.Log.e("DownloadWorker", "Download failed for $modelId, retrying...")
                Result.retry()
            }
        } catch (e: CancellationException) {
            android.util.Log.i("DownloadWorker", "Download cancelled for $modelId")
            throw e
        } catch (e: Exception) {
            android.util.Log.e("DownloadWorker", "Exception in DownloadWorker for $modelId", e)
            Result.retry()
        }
    }

    private fun createForegroundInfo(modelId: String): ForegroundInfo {
        val title = "Downloading TTS Model"
        val content = "Downloading $modelId..."

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW,
                )
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(title)
                .setTicker(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
