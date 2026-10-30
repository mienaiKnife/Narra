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
package com.mienaiknife.narra.playback

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager

class PowerLockManager(context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    fun acquireLocks() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                setReferenceCounted(false)
            }
        }
        // Re-acquiring refreshes the timeout. PlaybackService renews the lock while playback is
        // active, so the timeout only expires if the lock is ever leaked.
        wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
        if (wifiLock == null) {
            wifiLock = wifiManager.createWifiLock(WIFI_LOCK_MODE, WIFI_LOCK_TAG).apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    fun releaseLocks() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        wifiLock?.let { if (it.isHeld) it.release() }
        wifiLock = null
    }

    private companion object {
        const val WAKE_LOCK_TAG = "Narra:PlaybackWakeLock"
        const val WIFI_LOCK_TAG = "Narra:PlaybackWifiLock"
        const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L

        // WIFI_MODE_FULL_HIGH_PERF is deprecated in favour of WIFI_MODE_FULL_LOW_LATENCY, but the
        // replacement is only held while the screen is on, so it cannot keep Wi-Fi awake for
        // screen-off playback. Keep the legacy mode; newer platform releases already substitute
        // the low-latency mode automatically.
        @Suppress("DEPRECATION")
        const val WIFI_LOCK_MODE = WifiManager.WIFI_MODE_FULL_HIGH_PERF
    }
}
