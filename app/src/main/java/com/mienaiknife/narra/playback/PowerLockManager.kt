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
    private var manualWakeLock: PowerManager.WakeLock? = null

    fun acquireLocks() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Narra:PlaybackWakeLock").apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L /*10 minutes*/)
            }
        }
        if (wifiLock == null) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Narra:PlaybackWifiLock").apply {
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

    fun acquireManualWakeLock() {
        if (manualWakeLock == null) {
            manualWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Narra:ManualWakeLock").apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L /*10 minutes*/)
            }
        }
    }

    fun releaseManualWakeLock() {
        manualWakeLock?.let { if (it.isHeld) it.release() }
        manualWakeLock = null
    }
}
