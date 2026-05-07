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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import android.view.KeyEvent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mienaiknife.narra.MainActivity
import com.mienaiknife.narra.playback.PlaybackManager
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.playback.TtsPlayer
import com.mienaiknife.narra.ui.theme.ThemeManager
import com.mienaiknife.narra.ui.widget.WidgetManager
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var ttsPlayer: TtsPlayer

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var widgetManager: WidgetManager

    @Inject
    lateinit var themeManager: ThemeManager

    private var mediaSession: MediaSession? = null
    private lateinit var notificationProvider: MediaNotification.Provider
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private data class WidgetState(
        val isPlaying: Boolean,
        val article: Article?,
        val currentPosition: Long,
        val duration: Long,
        val playbackSpeed: Float,
        val showRemainingTime: Boolean,
    )

    companion object {
        private const val NOTIFICATION_ID = 3000
        private const val CHANNEL_ID = "playback_urgent"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        serviceScope.launch {
            combine(
                playbackManager.isPlaying,
                playbackManager.currentArticle,
                playbackManager.currentPosition,
                playbackManager.duration,
                playbackManager.playbackSpeed,
                themeManager.showRemainingTime
            ) { array ->
                WidgetState(
                    isPlaying = array[0] as Boolean,
                    article = array[1] as Article?,
                    currentPosition = array[2] as Long,
                    duration = array[3] as Long,
                    playbackSpeed = array[4] as Float,
                    showRemainingTime = array[5] as Boolean
                )
            }.collect { state ->
                val calculatedProgress = if (state.duration > 0) {
                    state.currentPosition.toFloat() / state.duration.toFloat()
                } else {
                    state.article?.progress ?: 0f
                }

                widgetManager.updateState(
                    isPlaying = state.isPlaying,
                    title = state.article?.title,
                    source = state.article?.source,
                    imageUrl = state.article?.imageUrl ?: state.article?.feedImageUrl,
                    progress = calculatedProgress,
                    duration = state.duration,
                    showRemainingTime = state.showRemainingTime,
                    playbackSpeed = state.playbackSpeed,
                )
            }
        }
        
        // Initialize TtsPlayer with speech-specific audio attributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        ttsPlayer.setAudioAttributes(audioAttributes, true)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .build()
        setMediaNotificationProvider(notificationProvider)

        val sessionExtras = Bundle().apply {
            // Standard extras to help Android recognize the app's media capabilities
            putBoolean("android.media.IS_EXPLICIT", false)
        }

        mediaSession = MediaSession.Builder(this, ttsPlayer)
            .setSessionActivity(pendingIntent)
            .setExtras(sessionExtras)
            .setCallback(
                object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    // Accept all controllers and allow all commands by default
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS)
                        .setAvailablePlayerCommands(session.player.availableCommands)
                        .build()
                }

                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent
                ): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }

                    if (keyEvent != null && (keyEvent.action == KeyEvent.ACTION_DOWN)) {
                        android.util.Log.d("PlaybackService", "Media button down: ${keyEvent.keyCode} from ${controllerInfo.packageName}")
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                playbackManager.togglePlayPause()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                ttsPlayer.play()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                ttsPlayer.pause()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                playbackManager.handleHardwareButton(isNext = true)
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                playbackManager.handleHardwareButton(isNext = false)
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(session, controllerInfo, intent)
                }

                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    isForPlayback: Boolean
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    android.util.Log.d("PlaybackService", "onPlaybackResumption called (isForPlayback=$isForPlayback)")
                    
                    ttsPlayer.currentMediaItem?.let { item ->
                        return Futures.immediateFuture(
                            MediaSession.MediaItemsWithStartPosition(
                                listOf(item),
                                0,
                                ttsPlayer.currentPosition
                            )
                        )
                    }
                    
                    return super.onPlaybackResumption(mediaSession, controller, isForPlayback)
                }
            })
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val metadata = ttsPlayer.currentMediaItem?.mediaMetadata
        val title = intent?.getStringExtra("EXTRA_TITLE") ?: metadata?.title?.toString() ?: "Narra"
        val text = intent?.getStringExtra("EXTRA_ARTIST") ?: metadata?.artist?.toString() ?: "Preparing playback..."

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.mienaiknife.narra.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Failed to start foreground: ${e.message}")
        }

        val result = super.onStartCommand(intent, flags, startId)
        
        // Signal Media3 to update the notification
        mediaSession?.let { onUpdateNotification(it, startInForegroundRequired = true) }
        
        return result
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent media controls"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        android.util.Log.d("PlaybackService", "onUpdateNotification: startInForegroundRequired=$startInForegroundRequired, isPlaying=${session.player.isPlaying}")
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || (!player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
