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
import android.os.Bundle
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import android.view.KeyEvent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
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
        private const val CHANNEL_ID = "playback_urgent_v2"
        
        const val CUSTOM_COMMAND_SKIP_FORWARD = "com.mienaiknife.narra.SKIP_FORWARD"
        const val CUSTOM_COMMAND_SKIP_BACKWARD = "com.mienaiknife.narra.SKIP_BACKWARD"
        
        const val ROOT_ID = "narra_root"
        const val CATEGORY_QUEUE = "category_queue"
        const val CATEGORY_INBOX = "category_inbox"
        const val CATEGORY_HISTORY = "category_history"
        const val CATEGORY_FAVORITES = "category_favorites"
    }

    override fun onCreate() {
        android.util.Log.d("PlaybackService", "onCreate called")
        super.onCreate()
        createNotificationChannel()
        
        // Samsung compatibility listener
        setListener(object : Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                android.util.Log.e("PlaybackService", "Foreground service start not allowed by system (Samsung/Battery optimization)")
            }
        })

        serviceScope.launch {
            combine(
                playbackManager.isPlaying,
                playbackManager.currentArticle,
                playbackManager.currentPosition,
                playbackManager.duration,
                playbackManager.playbackSpeed,
                themeManager.showRemainingTime,
            ) { array ->
                WidgetState(
                    isPlaying = array[0] as Boolean,
                    article = array[1] as Article?,
                    currentPosition = array[2] as Long,
                    duration = array[3] as Long,
                    playbackSpeed = array[4] as Float,
                    showRemainingTime = array[5] as Boolean,
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
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Set default notification provider with custom configuration
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationIdProvider { 1000 }
            .build()
        
        setMediaNotificationProvider(notificationProvider)

        val sessionExtras = Bundle().apply {
            // Standard extras to help Android recognize the app's media capabilities
            putBoolean("android.media.IS_EXPLICIT", false)
        }

        mediaSession = MediaLibrarySession.Builder(
            this,
            ttsPlayer,
            object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(CUSTOM_COMMAND_SKIP_FORWARD, Bundle.EMPTY))
                    .add(SessionCommand(CUSTOM_COMMAND_SKIP_BACKWARD, Bundle.EMPTY))
                    .build()
                
                val availablePlayerCommands = session.player.availableCommands.buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .build()
                
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(availablePlayerCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
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
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle("Narra")
                            .build(),
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                if (parentId == ROOT_ID) {
                    val categories = listOf(
                        createCategoryItem(CATEGORY_QUEUE, "Queue"),
                        createCategoryItem(CATEGORY_INBOX, "Inbox"),
                        createCategoryItem(CATEGORY_HISTORY, "History"),
                        createCategoryItem(CATEGORY_FAVORITES, "Favorites"),
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
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle(title)
                            .build(),
                    )
                    .build()
            }

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent,
            ): Boolean {
                val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }

                if (keyEvent != null && (keyEvent.action == KeyEvent.ACTION_DOWN)) {
                    android.util.Log.d("PlaybackService", "Media button down: ${keyEvent.keyCode} from ${controllerInfo.packageName} (Legacy: ${controllerInfo.controllerVersion == 0})")
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
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
                isForPlayback: Boolean,
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                android.util.Log.d("PlaybackService", "onPlaybackResumption called (isForPlayback=$isForPlayback)")
                
                val currentItem = ttsPlayer.currentMediaItem
                currentItem?.let {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            listOf(it),
                            0,
                            ttsPlayer.currentPosition,
                        ),
                    )
                }
                
                // Attempt to reload the last article if nothing is loaded
                val future = com.google.common.util.concurrent.SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                serviceScope.launch {
                    val reloaded = playbackManager.reloadLastArticle()
                    val newItem = ttsPlayer.currentMediaItem
                    if (reloaded && (newItem != null)) {
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(
                                listOf(newItem),
                                0,
                                ttsPlayer.currentPosition,
                            ),
                        )
                    } else {
                        future.setException(Exception("No article to resume"))
                    }
                }
                return future
            }
            },
        )
            .setSessionActivity(pendingIntent)
            .setExtras(sessionExtras)
            .build()
            
        // Promote to foreground immediately in onCreate to prevent crash
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val currentArticle = playbackManager.currentArticle.value
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(androidx.media3.session.R.drawable.media3_notification_small_icon)
                .setContentTitle(currentArticle?.title ?: "Narra")
                .setContentText("Preparing playback...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
            
            // Try to apply MediaStyle if session is available
            mediaSession?.let { session ->
                builder.setStyle(androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session))
            }
            
            val placeholderNotification = builder.build()
            
            try {
                // Use ID 1000 which matches Media3's DefaultMediaNotificationProvider.ID_DEFAULT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(1000, placeholderNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(1000, placeholderNotification)
                }
                android.util.Log.d("PlaybackService", "Manual startForeground(1000) with MediaStyle in onCreate")
            } catch (e: Exception) {
                android.util.Log.e("PlaybackService", "Failed to startForeground in onCreate", e)
            }
        }
        
        // Ensure session is aware of the current state immediately
        ttsPlayer.triggerStateInvalidation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("PlaybackService", "onStartCommand: intent=$intent")
        
        // Trigger a state report to ensure Media3 updates the notification
        ttsPlayer.triggerStateInvalidation()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Media controls for background playback"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(false) // Media notifications don't usually need badges
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        android.util.Log.d("PlaybackService", "onUpdateNotification: startInForegroundRequired=$startInForegroundRequired, isPlaying=${session.player.isPlaying}, state=${session.player.playbackState}")
        
        if (startInForegroundRequired && session.player.playbackState == Player.STATE_IDLE) {
            // Media3 might fail to post a notification if the player is idle.
            // By returning from here, we let the super class handle it, but we've logged it.
            // In TtsPlayer, we've added logic to return STATE_BUFFERING and a dummy item 
            // during preparation, which should prevent this case.
            android.util.Log.w("PlaybackService", "Service start in foreground required but player is IDLE")
        }

        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if ((player == null) || (!player.playWhenReady || (player.playbackState == Player.STATE_IDLE) || (player.playbackState == Player.STATE_ENDED))) {
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
