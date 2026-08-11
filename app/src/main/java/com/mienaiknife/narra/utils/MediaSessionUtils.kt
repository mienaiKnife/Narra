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
package com.mienaiknife.narra.utils

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.mienaiknife.narra.service.NarraMediaButtonReceiver

/**
 * Utility for performing low-level MediaSession operations, including reflection-based
 * workarounds for Samsung device compatibility.
 */
@UnstableApi
object MediaSessionUtils {
    private const val TAG = "MediaSessionUtils"
    private var lastPingTime = 0L
    private const val MIN_PING_INTERVAL_MS = 1000L

    /**
     * Forces the MediaSession to update its MediaButtonReceiver and activation state
     * via reflection. This is specifically needed for Samsung devices when using Media3 1.1+,
     * as the underlying MediaSessionCompat is no longer directly accessible.
     */
    fun forceActivationAndMbr(context: Context, session: MediaSession) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPingTime < MIN_PING_INTERVAL_MS) {
            return
        }
        lastPingTime = currentTime

        try {
            // Step 1: MediaSession.getImpl()
            val getImplMethod = session.javaClass.getDeclaredMethod("getImpl")
            getImplMethod.isAccessible = true
            val impl = getImplMethod.invoke(session) ?: return

            // Step 2: MediaSessionImpl.sessionLegacyStub
            var currentClass: Class<*>? = impl.javaClass
            var sessionLegacyStubField: java.lang.reflect.Field? = null
            while (currentClass != null && sessionLegacyStubField == null) {
                try {
                    sessionLegacyStubField = currentClass.getDeclaredField("sessionLegacyStub")
                } catch (e: NoSuchFieldException) {
                    currentClass = currentClass.superclass
                }
            }

            if (sessionLegacyStubField == null) {
                return
            }

            sessionLegacyStubField.isAccessible = true
            val sessionLegacyStub = sessionLegacyStubField.get(impl) ?: return

            // Step 3: MediaSessionLegacyStub.getSessionCompat()
            val getSessionCompatMethod = sessionLegacyStub.javaClass.getDeclaredMethod("getSessionCompat")
            getSessionCompatMethod.isAccessible = true
            val sessionCompat = getSessionCompatMethod.invoke(sessionLegacyStub) ?: run {
                android.util.Log.e(TAG, "Could not get sessionCompat")
                return
            }
            android.util.Log.v(TAG, "Found sessionCompat: ${sessionCompat.javaClass.name}")

            // Step 4: Construct MUTABLE MBR PendingIntent
            val mbrIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
            mbrIntent.setComponent(ComponentName(context, NarraMediaButtonReceiver::class.java))
            val mbrFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val mbrPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                mbrIntent,
                mbrFlags,
            )

            // Step 5: Set MBR on MediaSessionCompat
            val setMbrMethod = sessionCompat.javaClass.getDeclaredMethod("setMediaButtonReceiver", PendingIntent::class.java)
            setMbrMethod.invoke(sessionCompat, mbrPendingIntent)

            // Step 6: Force legacy flags for hardware buttons
            try {
                val setFlagsMethod = sessionCompat.javaClass.getDeclaredMethod("setFlags", Int::class.javaPrimitiveType ?: Int::class.java)
                setFlagsMethod.invoke(sessionCompat, 3)
            } catch (e: Exception) {}

            // Step 7: Push aggressive extras to legacy session
            try {
                val setExtrasMethod = sessionCompat.javaClass.getDeclaredMethod("setExtras", Bundle::class.java)
                val extras = Bundle().apply {
                    putBoolean("android.media.IS_EXPLICIT", true)
                    putLong("android.media.IS_EXPLICIT", 1L)
                    putBoolean("android.media.session.extra.EXTRA_SLOT_RESERVATION", true)
                    putLong("android.media.session.extra.EXTRA_SLOT_RESERVATION", 3L)
                    putBoolean("android.media.session.extra.EXTRA_RESERVE_PLAY_PAUSE", true)
                    putLong("android.media.session.extra.EXTRA_RESERVE_PLAY_PAUSE", 1L)
                    putBoolean("android.media.session.extra.RESERVE_PLAY_PAUSE", true)
                    putLong("android.media.session.extra.RESERVE_PLAY_PAUSE", 1L)
                    putBoolean("android.media.session.extra.RESERVE_SKIP_NEXT", true)
                    putLong("android.media.session.extra.RESERVE_SKIP_NEXT", 1L)
                    putBoolean("android.media.session.extra.RESERVE_SKIP_PREV", true)
                    putLong("android.media.session.extra.RESERVE_SKIP_PREV", 1L)
                    putParcelable("android.media.session.extra.MEDIA_BUTTON_RECEIVER", mbrPendingIntent)
                    putString("android.media.session.extra.KEY_EVENT_RECEIVER_PACKAGE", context.packageName)
                    putString("android.media.session.extra.KEY_EVENT_RECEIVER_CLASS", NarraMediaButtonReceiver::class.java.name)
                }
                setExtrasMethod.invoke(sessionCompat, extras)
            } catch (e: Exception) {}

            // Step 8: Set metadata from player
            try {
                val metadataClassName = when {
                    isClassAvailable("androidx.media3.session.legacy.MediaMetadataCompat") -> "androidx.media3.session.legacy.MediaMetadataCompat"
                    isClassAvailable("androidx.media.MediaMetadataCompat") -> "androidx.media.MediaMetadataCompat"
                    else -> null
                }

                if (metadataClassName != null) {
                    val metadataClass = Class.forName(metadataClassName)
                    val builderClass = Class.forName("$metadataClassName\$Builder")
                    val builder = builderClass.getDeclaredConstructor().newInstance()
                    val putStringMethod = builderClass.getDeclaredMethod("putString", String::class.java, String::class.java)
                    val putLongMethod = builderClass.getDeclaredMethod("putLong", String::class.java, Long::class.javaPrimitiveType ?: Long::class.java)

                    val playerMetadata = session.player.mediaMetadata
                    val title = playerMetadata.title?.toString() ?: "Narra"
                    val artist = playerMetadata.artist?.toString() ?: playerMetadata.subtitle?.toString() ?: "Narra"

                    putStringMethod.invoke(builder, "android.media.metadata.TITLE", title)
                    putStringMethod.invoke(builder, "android.media.metadata.ARTIST", artist)
                    putStringMethod.invoke(builder, "android.media.metadata.MEDIA_ID", session.player.currentMediaItem?.mediaId ?: "narra_active_session")

                    val duration = session.player.duration
                    if (duration > 0) {
                        putLongMethod.invoke(builder, "android.media.metadata.DURATION", duration)
                    } else {
                        putLongMethod.invoke(builder, "android.media.metadata.DURATION", 3600000L)
                    }

                    val metadata = builderClass.getDeclaredMethod("build").invoke(builder)
                    val setMetadataMethod = sessionCompat.javaClass.getDeclaredMethod("setMetadata", metadataClass)
                    setMetadataMethod.invoke(sessionCompat, metadata)
                }
            } catch (e: Exception) {}

            // Step 9: Force AGGRESSIVE dynamic state
            try {
                val stateClassName = when {
                    isClassAvailable("androidx.media3.session.legacy.PlaybackStateCompat") -> "androidx.media3.session.legacy.PlaybackStateCompat"
                    isClassAvailable("androidx.media.session.PlaybackStateCompat") -> "androidx.media.session.PlaybackStateCompat"
                    else -> null
                }

                if (stateClassName != null) {
                    val playbackStateClass = Class.forName(stateClassName)
                    val builderClass = Class.forName("$stateClassName\$Builder")
                    val builder = builderClass.getDeclaredConstructor().newInstance()
                    val setStateMethod = builderClass.getDeclaredMethod("setState", Int::class.javaPrimitiveType ?: Int::class.java, Long::class.javaPrimitiveType ?: Long::class.java, Float::class.javaPrimitiveType ?: Float::class.java)

                    val player = session.player
                    val legacyState = when (player.playbackState) {
                        androidx.media3.common.Player.STATE_READY -> if (player.playWhenReady) 3 else 2 // 3: PLAYING, 2: PAUSED
                        androidx.media3.common.Player.STATE_BUFFERING -> 6 // 6: BUFFERING
                        androidx.media3.common.Player.STATE_ENDED -> 1 // 1: STOPPED
                        else -> 0 // 0: NONE
                    }

                    setStateMethod.invoke(builder, legacyState, player.currentPosition, player.playbackParameters.speed)

                    val setActionsMethod = builderClass.getDeclaredMethod("setActions", Long::class.javaPrimitiveType ?: Long::class.java)
                    setActionsMethod.invoke(builder, 3967L) // Standard actions + Preparations (895L -> 3967L)

                    val state = builderClass.getDeclaredMethod("build").invoke(builder)
                    val setPlaybackStateMethod = sessionCompat.javaClass.getDeclaredMethod("setPlaybackState", playbackStateClass)
                    setPlaybackStateMethod.invoke(sessionCompat, state)
                }
            } catch (e: Exception) {}

            // Step 10: Ensure Active with Pulse
            val setActiveMethod = sessionCompat.javaClass.getDeclaredMethod("setActive", Boolean::class.javaPrimitiveType ?: Boolean::class.java)
            setActiveMethod.invoke(sessionCompat, false)
            setActiveMethod.invoke(sessionCompat, true)

            android.util.Log.v(TAG, "Samsung priority claim complete")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed Samsung workaround", e)
        }
    }

    private fun isClassAvailable(className: String): Boolean = try {
        Class.forName(className)
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}
