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
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
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
class PlaybackService : MediaLibraryService() {

    @Inject
    lateinit var ttsPlayer: TtsPlayer

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var widgetManager: WidgetManager

    @Inject
    lateinit var themeManager: ThemeManager

    private var mediaSession: MediaLibrarySession? = null
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
        
        const val CUSTOM_COMMAND_SKIP_FORWARD = "com.mienaiknife.narra.SKIP_FORWARD"
        const val CUSTOM_COMMAND_SKIP_BACKWARD = "com.mienaiknife.narra.SKIP_BACKWARD"
        
        const val ROOT_ID = "narra_root"
        const val CATEGORY_QUEUE = "category_queue"
        const val CATEGORY_INBOX = "category_inbox"
        const val CATEGORY_HISTORY = "category_history"
        const val CATEGORY_FAVORITES = "category_favorites"
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

        notificationProvider = object : MediaNotification.Provider {
            private val defaultProvider = DefaultMediaNotificationProvider.Builder(this@PlaybackService)
                .setChannelId(CHANNEL_ID)
                .build()

            override fun createNotification(
                session: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                val skipForwardButton = CommandButton.Builder()
                    .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SKIP_FORWARD, Bundle.EMPTY))
                    .setDisplayName("Forward")
                    .setIconResId(com.mienaiknife.narra.R.drawable.ic_fast_forward)
                    .setEnabled(true)
                    .build()
                
                val skipBackwardButton = CommandButton.Builder()
                    .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SKIP_BACKWARD, Bundle.EMPTY))
                    .setDisplayName("Backward")
                    .setIconResId(com.mienaiknife.narra.R.drawable.ic_rewind)
                    .setEnabled(true)
                    .build()

                val customLayoutWithSkips = ImmutableList.of(
                    skipBackwardButton,
                    skipForwardButton
                )

                return defaultProvider.createNotification(
                    session,
                    customLayoutWithSkips,
                    actionFactory,
                    onNotificationChangedCallback
                )
            }

            override fun handleCustomCommand(
                session: MediaSession,
                action: String,
                extras: Bundle
            ): Boolean = false

            override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
                return defaultProvider.getNotificationChannelInfo()
            }
        }
        setMediaNotificationProvider(notificationProvider)

        val sessionExtras = Bundle().apply {
            // Standard extras to help Android recognize the app's media capabilities
            putBoolean("android.media.IS_EXPLICIT", false)
        }

        mediaSession = MediaLibrarySession.Builder(this, ttsPlayer, object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(CUSTOM_COMMAND_SKIP_FORWARD, Bundle.EMPTY))
                    .add(SessionCommand(CUSTOM_COMMAND_SKIP_BACKWARD, Bundle.EMPTY))
                    .build()
                
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(session.player.availableCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    CUSTOM_COMMAND_SKIP_FORWARD -> {
                        playbackManager.skipForward()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    CUSTOM_COMMAND_SKIP_BACKWARD -> {
                        playbackManager.skipBackward()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle("Narra")
                        .build())
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                if (parentId == ROOT_ID) {
                    val categories = listOf(
                        createCategoryItem(CATEGORY_QUEUE, "Queue"),
                        createCategoryItem(CATEGORY_INBOX, "Inbox"),
                        createCategoryItem(CATEGORY_HISTORY, "History"),
                        createCategoryItem(CATEGORY_FAVORITES, "Favorites")
                    )
                    return Futures.immediateFuture(LibraryResult.ofItemList(categories, params))
                }
                
                // For actual articles, we'd need to fetch from repository asynchronously
                // For now, return empty to avoid blocking the main thread significantly
                return Futures.immediateFuture(LibraryResult.ofItemList(listOf(), params))
            }

            private fun createCategoryItem(id: String, title: String): MediaItem {
                return MediaItem.Builder()
                    .setMediaId(id)
                    .setMediaMetadata(MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle(title)
                        .build())
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
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            playbackManager.skipForward()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            playbackManager.skipBackward()
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
                
                val currentItem = ttsPlayer.currentMediaItem
                if (currentItem != null) {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            listOf(currentItem),
                            0,
                            ttsPlayer.currentPosition
                        )
                    )
                }
                
                // Attempt to reload the last article if nothing is loaded
                val future = com.google.common.util.concurrent.SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                serviceScope.launch {
                    val reloaded = playbackManager.reloadLastArticle()
                    val newItem = ttsPlayer.currentMediaItem
                    if (reloaded && newItem != null) {
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(
                                listOf(newItem),
                                0,
                                ttsPlayer.currentPosition
                            )
                        )
                    } else {
                        future.setException(Exception("No article to resume"))
                    }
                }
                return future
            }
        })
            .setSessionActivity(pendingIntent)
            .setExtras(sessionExtras)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is android.app.ForegroundServiceStartNotAllowedException) {
                android.util.Log.e("PlaybackService", "Foreground service start not allowed: ${e.message}")
            } else {
                android.util.Log.e("PlaybackService", "Failed to start foreground: ${e.message}")
            }
        }

        // Signal Media3 to update the notification if session is available
        mediaSession?.let { onUpdateNotification(it, startInForegroundRequired = false) }
        
        return super.onStartCommand(intent, flags, startId)
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
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
