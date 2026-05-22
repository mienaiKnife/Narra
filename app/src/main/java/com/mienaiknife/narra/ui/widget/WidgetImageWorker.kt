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

package com.mienaiknife.narra.ui.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class WidgetImageWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val imageUrl = inputData.getString("image_url") ?: return Result.failure()
        
        return try {
            val imageLoader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(400) // Thumbnails are enough for widgets
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.image.toBitmap()
                val file = File(context.cacheDir, "widget_artwork.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                
                updateWidgetState(file.absolutePath)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetImageWorker", "Error downloading widget image", e)
            Result.retry()
        }
    }

    private suspend fun updateWidgetState(path: String) {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIds(NarraWidget::class.java).firstOrNull() ?: return
        updateAppWidgetState(context, NarraWidget().stateDefinition, glanceId) { prefs ->
            val mutablePrefs = prefs.toMutablePreferences()
            mutablePrefs[WidgetManager.KEY_IMAGE_PATH] = path
            mutablePrefs
        }
        NarraWidget().update(context, glanceId)
    }
}
