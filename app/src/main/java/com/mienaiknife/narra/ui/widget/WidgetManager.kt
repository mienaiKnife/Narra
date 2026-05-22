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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        val KEY_IS_PLAYING = booleanPreferencesKey("is_playing")
        val KEY_ARTICLE_ID = stringPreferencesKey("article_id")
        val KEY_ARTICLE_TITLE = stringPreferencesKey("article_title")
        val KEY_ARTICLE_SOURCE = stringPreferencesKey("article_source")
        val KEY_ARTICLE_IMAGE_URL = stringPreferencesKey("article_image_url")
        val KEY_IMAGE_PATH = stringPreferencesKey("image_path")
        val KEY_PROGRESS = floatPreferencesKey("progress")
        val KEY_DURATION = longPreferencesKey("duration")
        val KEY_SHOW_REMAINING_TIME = booleanPreferencesKey("show_remaining_time")
        val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    }

    suspend fun updateState(
        isPlaying: Boolean,
        articleId: String?,
        title: String?,
        source: String?,
        imageUrl: String?,
        progress: Float?,
        duration: Long?,
        showRemainingTime: Boolean,
        playbackSpeed: Float,
    ) {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIds(NarraWidget::class.java).firstOrNull() ?: return
        
        var shouldDownloadImage = false
        
        updateAppWidgetState(context, NarraWidget().stateDefinition, glanceId) { prefs ->
            val mutablePrefs = prefs.toMutablePreferences()
            val currentImageUrl = prefs[KEY_ARTICLE_IMAGE_URL] ?: ""
            if (imageUrl != null && imageUrl != currentImageUrl) {
                shouldDownloadImage = true
            } else if (imageUrl == null && currentImageUrl.isNotEmpty()) {
                // Clear image path if no image url
                mutablePrefs.remove(KEY_IMAGE_PATH)
            }

            mutablePrefs.apply {
                this[KEY_IS_PLAYING] = isPlaying
                this[KEY_ARTICLE_ID] = articleId ?: ""
                this[KEY_ARTICLE_TITLE] = title ?: ""
                this[KEY_ARTICLE_SOURCE] = source ?: ""
                this[KEY_ARTICLE_IMAGE_URL] = imageUrl ?: ""
                this[KEY_PROGRESS] = progress ?: 0f
                this[KEY_DURATION] = duration ?: 0L
                this[KEY_SHOW_REMAINING_TIME] = showRemainingTime
                this[KEY_PLAYBACK_SPEED] = playbackSpeed
            }
            mutablePrefs
        }
        
        if (shouldDownloadImage && imageUrl != null) {
            enqueueImageDownload(imageUrl)
        }
        
        NarraWidget().update(context, glanceId)
    }

    private fun enqueueImageDownload(imageUrl: String) {
        val data = Data.Builder()
            .putString("image_url", imageUrl)
            .build()
        
        val request = OneTimeWorkRequestBuilder<WidgetImageWorker>()
            .setInputData(data)
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_image_download",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
