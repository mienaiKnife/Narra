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
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.currentState
import androidx.glance.ColorFilter
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.background
import androidx.glance.material3.ColorProviders
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.theme.DarkColorScheme
import com.mienaiknife.narra.utils.DateUtils

class NarraWidget : GlanceAppWidget() {

    companion object {
        val DarkThemeColors = ColorProviders(DarkColorScheme)
    }

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            GlanceTheme(colors = DarkThemeColors) {
                WidgetContent(
                    title = prefs[WidgetManager.KEY_ARTICLE_TITLE] ?: "No article playing",
                    isPlaying = prefs[WidgetManager.KEY_IS_PLAYING] ?: false,
                    progress = prefs[WidgetManager.KEY_PROGRESS] ?: 0f,
                    duration = prefs[WidgetManager.KEY_DURATION] ?: 0L,
                    showRemainingTime = prefs[WidgetManager.KEY_SHOW_REMAINING_TIME] ?: true,
                    playbackSpeed = prefs[WidgetManager.KEY_PLAYBACK_SPEED] ?: 1.0f
                )
            }
        }
    }

    @Composable
    fun WidgetContent(
        title: String,
        isPlaying: Boolean,
        progress: Float,
        duration: Long,
        showRemainingTime: Boolean,
        playbackSpeed: Float
    ) {
        val totalDurationAtSpeed = (duration / playbackSpeed).toLong()
        val currentPosition = (progress * totalDurationAtSpeed).toLong()
        val remainingTime = totalDurationAtSpeed - currentPosition

        val timeText = if (totalDurationAtSpeed > 0) {
            val elapsedStr = DateUtils.formatElapsedTime(currentPosition, totalDurationAtSpeed)
            val durationStr = if (showRemainingTime) {
                "-${DateUtils.formatElapsedTime(remainingTime, totalDurationAtSpeed)}"
            } else {
                DateUtils.formatElapsedTime(totalDurationAtSpeed, totalDurationAtSpeed)
            }
            "$elapsedStr / $durationStr"
        } else ""

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(8.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Article Icon
                // Remote images require manual downloading to Bitmap in Glance 1.1.1.
                // For now, we use a placeholder.
                Box(
                    modifier = GlanceModifier
                        .size(64.dp)
                        .cornerRadius(12.dp)
                        .background(GlanceTheme.colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_foreground),
                        contentDescription = "Placeholder",
                        modifier = GlanceModifier.size(48.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Content Column
                Column(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Row 1: Title
                    Text(
                        text = title,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )

                    // Row 2: Time
                    if (timeText.isNotEmpty()) {
                        Text(
                            text = timeText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface
                            ),
                            maxLines = 1
                        )
                    }

                    // Row 3: Controls
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Image(
                            provider = ImageProvider(R.drawable.ic_rewind),
                            contentDescription = "Rewind",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                            modifier = GlanceModifier.size(32.dp).clickable(
                                actionRunCallback<PlaybackActionCallback>(
                                    PlaybackActionCallback.createParameters(PlaybackActionCallback.ACTION_SKIP_BACKWARD)
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Image(
                            provider = ImageProvider(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                            modifier = GlanceModifier.size(32.dp).clickable(
                                actionRunCallback<PlaybackActionCallback>(
                                    PlaybackActionCallback.createParameters(PlaybackActionCallback.ACTION_TOGGLE)
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Image(
                            provider = ImageProvider(R.drawable.ic_fast_forward),
                            contentDescription = "Fast Forward",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                            modifier = GlanceModifier.size(32.dp).clickable(
                                actionRunCallback<PlaybackActionCallback>(
                                    PlaybackActionCallback.createParameters(PlaybackActionCallback.ACTION_SKIP_FORWARD)
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Image(
                            provider = ImageProvider(R.drawable.ic_skip_next),
                            contentDescription = "Skip Next",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                            modifier = GlanceModifier.size(32.dp).clickable(
                                actionRunCallback<PlaybackActionCallback>(
                                    PlaybackActionCallback.createParameters(PlaybackActionCallback.ACTION_SKIP_NEXT)
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 80)
@Composable
fun NarraWidgetPreview() {
    GlanceTheme(colors = NarraWidget.DarkThemeColors) {
        NarraWidget().WidgetContent(
            title = "Extremely Long Article Title That Should Be Truncated",
            isPlaying = true,
            progress = 0.5f,
            duration = 3600000L,
            showRemainingTime = true,
            playbackSpeed = 1.0f
        )
    }
}
