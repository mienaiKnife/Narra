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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        val KEY_IS_PLAYING = booleanPreferencesKey("is_playing")
        val KEY_ARTICLE_TITLE = stringPreferencesKey("article_title")
        val KEY_ARTICLE_SOURCE = stringPreferencesKey("article_source")
        val KEY_ARTICLE_IMAGE_URL = stringPreferencesKey("article_image_url")
        val KEY_PROGRESS = floatPreferencesKey("progress")
        val KEY_DURATION = longPreferencesKey("duration")
        val KEY_SHOW_REMAINING_TIME = booleanPreferencesKey("show_remaining_time")
        val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    }

    suspend fun updateState(
        isPlaying: Boolean,
        title: String?,
        source: String?,
        imageUrl: String?,
        progress: Float?,
        duration: Long?,
        showRemainingTime: Boolean,
        playbackSpeed: Float,
    ) {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIds(NarraWidget::class.java).firstOrNull() ?: return
        
        updateAppWidgetState(context, NarraWidget().stateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[KEY_IS_PLAYING] = isPlaying
                this[KEY_ARTICLE_TITLE] = title ?: ""
                this[KEY_ARTICLE_SOURCE] = source ?: ""
                this[KEY_ARTICLE_IMAGE_URL] = imageUrl ?: ""
                this[KEY_PROGRESS] = progress ?: 0f
                this[KEY_DURATION] = duration ?: 0L
                this[KEY_SHOW_REMAINING_TIME] = showRemainingTime
                this[KEY_PLAYBACK_SPEED] = playbackSpeed
            }
        }
        NarraWidget().update(context, glanceId)
    }
}
