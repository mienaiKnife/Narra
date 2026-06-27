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
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import androidx.media3.session.MediaSession

/**
 * Utility for performing low-level MediaSession operations, including reflection-based
 * workarounds for Samsung device compatibility.
 */
@UnstableApi
object MediaSessionUtils {
    private const val TAG = "MediaSessionUtils"

    /**
     * Forces the MediaSession to update its MediaButtonReceiver and activation state
     * via reflection. This is specifically needed for Samsung devices when using Media3 1.1+,
     * as the underlying MediaSessionCompat is no longer directly accessible.
     */
    fun forceActivationAndMbr(context: Context, session: MediaSession) {
        try {
            // Step 1: MediaSession.getImpl()
            val getImplMethod = session.javaClass.getDeclaredMethod("getImpl")
            getImplMethod.isAccessible = true
            val impl = getImplMethod.invoke(session) ?: return

            // Step 2: MediaSessionImpl.sessionLegacyStub
            // Note: MediaSessionImpl is internal, we need to traverse the hierarchy
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
                android.util.Log.e(TAG, "Could not find sessionLegacyStub field")
                return
            }
            
            sessionLegacyStubField.isAccessible = true
            val sessionLegacyStub = sessionLegacyStubField.get(impl) ?: return

            // Step 3: MediaSessionLegacyStub.getSessionCompat()
            val getSessionCompatMethod = sessionLegacyStub.javaClass.getDeclaredMethod("getSessionCompat")
            getSessionCompatMethod.isAccessible = true
            val sessionCompat = getSessionCompatMethod.invoke(sessionLegacyStub) ?: return

            // Step 4: Construct MBR PendingIntent
            val mbrIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
            mbrIntent.setComponent(ComponentName(context, MediaButtonReceiver::class.java))
            val mbrPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                mbrIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            )

            // Step 5: Call setMediaButtonReceiver on MediaSessionCompat
            val setMbrMethod = sessionCompat.javaClass.getDeclaredMethod("setMediaButtonReceiver", PendingIntent::class.java)
            setMbrMethod.invoke(sessionCompat, mbrPendingIntent)

            // Step 6: Force setActive(true) to claim focus
            // On some Samsung devices, cycling the active state helps the system notice the change
            val setActiveMethod = sessionCompat.javaClass.getDeclaredMethod("setActive", Boolean::class.java)
            setActiveMethod.invoke(sessionCompat, false)
            setActiveMethod.invoke(sessionCompat, true)

            android.util.Log.d(TAG, "Successfully forced MBR and Activation via reflection (cycled state)")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to force MBR/Activation via reflection", e)
        }
    }
}
