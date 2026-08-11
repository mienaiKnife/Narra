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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
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
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mienaiknife.narra.MainActivity
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.playback.PlaybackManager
import com.mienaiknife.narra.playback.TtsPlayer
import com.mienaiknife.narra.ui.theme.ThemeManager
import com.mienaiknife.narra.ui.widget.PlaybackActionCallback
import com.mienaiknife.narra.ui.widget.WidgetManager
import com.mienaiknife.narra.utils.MediaSessionUtils
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

    private var isForegrounded = false
    private var silenceTrack: AudioTrack? = null
    private var isSilenceRunning = false

    private data class WidgetState(
        val isPlaying: Boolean,
        val article: Article?,
        val currentPosition: Long,
        val duration: Long,
        val playbackSpeed: Float,
        val showRemainingTime: Boolean,
    )

    companion object {
        private const val CHANNEL_ID = "playback_v13"
        private const val NOTIFICATION_ID = 1000

        const val CUSTOM_COMMAND_SKIP_FORWARD = "com.mienaiknife.narra.SKIP_FORWARD"
        const val CUSTOM_COMMAND_SKIP_BACKWARD = "com.mienaiknife.narra.SKIP_BACKWARD"

        const val ROOT_ID = "narra_root"
    }

    override fun onCreate() {
        android.util.Log.d("PlaybackService", "onCreate started")

        // Immediate startForeground to prevent crash
        createNotificationChannel()
        startForegroundEarly()

        super.onCreate()

        createMediaSession()
        mediaSession?.let { addSession(it) }

        // Trigger priority claim on state changes
        ttsPlayer.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    startSilence()
                    reinforceLegacyPriority()
                } else {
                    stopSilence()
                }
                mediaSession?.let { session ->
                    MediaSessionUtils.forceActivationAndMbr(this@PlaybackService, session)
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                mediaSession?.let { session ->
                    MediaSessionUtils.forceActivationAndMbr(this@PlaybackService, session)
                }
            }
        })

        // Samsung compatibility listener
        setListener(
            object : Listener {
                override fun onForegroundServiceStartNotAllowedException() {
                    android.util.Log.e("PlaybackService", "Foreground service start not allowed")
                }
            },
        )

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
                val calculatedProgress = if (state.duration > 0) state.currentPosition.toFloat() / state.duration.toFloat() else state.article?.progress ?: 0f
                widgetManager.updateState(
                    isPlaying = state.isPlaying,
                    articleId = state.article?.id,
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

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        ttsPlayer.setAudioAttributes(audioAttributes, true)

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationIdProvider { NOTIFICATION_ID }
            .build()
        setMediaNotificationProvider(notificationProvider)

        ttsPlayer.triggerStateInvalidation()

        // Samsung Priority Loop
        serviceScope.launch {
            kotlinx.coroutines.delay(2000)
            while (true) {
                mediaSession?.let { session ->
                    MediaSessionUtils.forceActivationAndMbr(this@PlaybackService, session)
                }
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    private fun startForegroundEarly() {
        if (isForegrounded) return

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Ready")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegrounded = true
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Failed startForegroundEarly", e)
        }
    }

    private fun createMediaSession() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val mbrIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mbrIntent.setComponent(ComponentName(this, NarraMediaButtonReceiver::class.java))
        val mbrFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        val mbrPendingIntent = PendingIntent.getBroadcast(this, 0, mbrIntent, mbrFlags)

        val sessionExtras = Bundle().apply {
            putBoolean("android.media.IS_EXPLICIT", true)
            putLong("android.media.IS_EXPLICIT", 1L)
            putBoolean("android.media.session.extra.EXTRA_SLOT_RESERVATION", true)
            putLong("android.media.session.extra.EXTRA_SLOT_RESERVATION", 3L)
            putBoolean("android.media.session.extra.RESERVE_PLAY_PAUSE", true)
            putLong("android.media.session.extra.RESERVE_PLAY_PAUSE", 1L)
            putBoolean("android.media.session.extra.RESERVE_SKIP_NEXT", true)
            putLong("android.media.session.extra.RESERVE_SKIP_NEXT", 1L)
            putBoolean("android.media.session.extra.RESERVE_SKIP_PREV", true)
            putLong("android.media.session.extra.RESERVE_SKIP_PREV", 1L)
            putParcelable("android.media.session.extra.MEDIA_BUTTON_RECEIVER", mbrPendingIntent)
            putString("android.media.session.extra.KEY_EVENT_RECEIVER_PACKAGE", packageName)
            putString("android.media.session.extra.KEY_EVENT_RECEIVER_CLASS", NarraMediaButtonReceiver::class.java.name)
        }

        mediaSession = MediaLibrarySession.Builder(
            this,
            ttsPlayer,
            object : MediaLibrarySession.Callback {
                override fun onPlaybackResumption(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    android.util.Log.d("PlaybackService", "onPlaybackResumption triggered")
                    val currentItem = ttsPlayer.currentMediaItem
                    return if (currentItem != null) {
                        Futures.immediateFuture(
                            MediaSession.MediaItemsWithStartPosition(
                                listOf(currentItem),
                                ttsPlayer.currentMediaItemIndex,
                                ttsPlayer.currentPosition,
                            ),
                        )
                    } else {
                        super.onPlaybackResumption(session, controller)
                    }
                }

                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult {
                    val availablePlayerCommands =
                        MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                            .addAll(session.player.availableCommands)
                            .add(Player.COMMAND_PLAY_PAUSE)
                            .add(Player.COMMAND_STOP)
                            .add(Player.COMMAND_SEEK_BACK)
                            .add(Player.COMMAND_SEEK_FORWARD)
                            .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailablePlayerCommands(availablePlayerCommands)
                        .setSessionExtras(sessionExtras)
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
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
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
                                .setTitle(getString(R.string.app_name))
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
                ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = Futures.immediateFuture(LibraryResult.ofItemList(listOf(), params))

                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    intent: Intent,
                ): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    android.util.Log.i("PlaybackService", "onMediaButtonEvent: $keyEvent")
                    if (keyEvent == null || keyEvent.action != KeyEvent.ACTION_DOWN) {
                        return super.onMediaButtonEvent(session, controller, intent)
                    }
                    return when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE,
                        KeyEvent.KEYCODE_HEADSETHOOK,
                        -> {
                            playbackManager.togglePlayPause()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            playbackManager.handleHardwareButton(isNext = true)
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            playbackManager.handleHardwareButton(isNext = false)
                            true
                        }
                        else -> super.onMediaButtonEvent(session, controller, intent)
                    }
                }
            },
        )
            .setSessionActivity(pendingIntent)
            .setSessionExtras(sessionExtras)
            .setId("NarraPlaybackSession")
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundEarly()
        when (intent?.action) {
            PlaybackActionCallback.ACTION_TOGGLE -> playbackManager.togglePlayPause()
            PlaybackActionCallback.ACTION_SKIP_FORWARD -> playbackManager.skipForward()
            PlaybackActionCallback.ACTION_SKIP_BACKWARD -> playbackManager.skipBackward()
            PlaybackActionCallback.ACTION_SKIP_NEXT -> playbackManager.skipNext()
        }
        mediaSession?.let { MediaSessionUtils.forceActivationAndMbr(this, it) }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.settings_playback_title), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || (!player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED)) stopSelf()
    }

    override fun onDestroy() {
        android.util.Log.d("PlaybackService", "onDestroy called")
        stopSilence()
        serviceScope.cancel()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun startSilence() {
        if (isSilenceRunning) return
        isSilenceRunning = true
        serviceScope.launch(Dispatchers.IO) {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                silenceTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                )
                val silence = ShortArray(bufferSize)
                silenceTrack?.play()
                while (isSilenceRunning) {
                    silenceTrack?.write(silence, 0, silence.size)
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackService", "Silence track error", e)
            } finally {
                silenceTrack?.stop()
                silenceTrack?.release()
                silenceTrack = null
            }
        }
    }

    private fun stopSilence() {
        isSilenceRunning = false
    }

    private fun reinforceLegacyPriority() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val componentName = ComponentName(this, NarraMediaButtonReceiver::class.java)
        @Suppress("DEPRECATION")
        audioManager.registerMediaButtonEventReceiver(componentName)
        android.util.Log.i("PlaybackService", "Reinforced legacy priority with AudioManager")
    }
}
