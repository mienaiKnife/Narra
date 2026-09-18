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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.mienaiknife.narra.data.local.AppDatabase
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.data.settings.SyncSettingsManager
import com.mienaiknife.narra.domain.repository.ImportExportRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class SyncManagerTest {
    private lateinit var workManager: WorkManager
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
        syncManager =
            SyncManager(
                appDatabase = mock<AppDatabase>(),
                syncSettingsManager = mock<SyncSettingsManager>(),
                downloadSettingsManager = mock<DownloadSettingsManager>(),
                importExportRepository = mock<ImportExportRepository>(),
                workManager = workManager,
            )
    }

    @Test
    fun `refreshIntervalMinutes maps every supported option`() {
        assertNull(SyncManager.refreshIntervalMinutes("Never"))
        assertEquals(60L, SyncManager.refreshIntervalMinutes("1 hour"))
        assertEquals(180L, SyncManager.refreshIntervalMinutes("3 hours"))
        assertEquals(360L, SyncManager.refreshIntervalMinutes("6 hours"))
        assertEquals(720L, SyncManager.refreshIntervalMinutes("12 hours"))
        assertEquals(1440L, SyncManager.refreshIntervalMinutes("24 hours"))
        assertEquals(
            SyncManager.DEFAULT_REFRESH_INTERVAL_MINUTES,
            SyncManager.refreshIntervalMinutes("unexpected"),
        )
    }

    @Test
    fun `periodicPolicyFor keeps the schedule until the configuration changes`() {
        assertEquals(
            ExistingPeriodicWorkPolicy.KEEP,
            SyncManager.periodicPolicyFor(previous = null, current = "1 hour" to true),
        )
        assertEquals(
            ExistingPeriodicWorkPolicy.KEEP,
            SyncManager.periodicPolicyFor(previous = "1 hour" to true, current = "1 hour" to true),
        )
        assertEquals(
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            SyncManager.periodicPolicyFor(previous = "1 hour" to true, current = "3 hours" to true),
        )
        assertEquals(
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            SyncManager.periodicPolicyFor(previous = "1 hour" to true, current = "1 hour" to false),
        )
    }

    @Test
    fun `scheduleFeedRefresh enqueues periodic work with the selected interval and wifi constraint`() {
        syncManager.scheduleFeedRefresh("3 hours", wifiOnly = true)

        val info = enqueuedInfos().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(3 * 60 * 60 * 1000L, info.repeatIntervalMillis())
        assertEquals(NetworkType.UNMETERED, info.constraints.requiredNetworkType)
    }

    @Test
    fun `scheduleFeedRefresh uses the connected constraint when wifi only is off`() {
        syncManager.scheduleFeedRefresh("1 hour", wifiOnly = false)

        val info = enqueuedInfos().single()
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
    }

    @Test
    fun `changing the interval re-enqueues the work with the new period`() {
        syncManager.scheduleFeedRefresh("1 hour", wifiOnly = false)
        syncManager.scheduleFeedRefresh("24 hours", wifiOnly = false)

        val info = enqueuedInfos().single()
        assertEquals(24 * 60 * 60 * 1000L, info.repeatIntervalMillis())
    }

    @Test
    fun `scheduleFeedRefresh with Never cancels the periodic work`() {
        syncManager.scheduleFeedRefresh("1 hour", wifiOnly = false)
        assertEquals(1, enqueuedInfos().size)

        syncManager.scheduleFeedRefresh("Never", wifiOnly = false)

        assertTrue("No background refresh should remain scheduled", enqueuedInfos().isEmpty())
    }

    private fun enqueuedInfos(): List<WorkInfo> = workManager
        .getWorkInfosForUniqueWork(SyncManager.FEED_REFRESH_WORK_NAME)
        .get()
        .filter { it.state == WorkInfo.State.ENQUEUED }

    private fun WorkInfo.repeatIntervalMillis(): Long = requireNotNull(periodicityInfo) { "Periodic work should expose a periodicity info" }.repeatIntervalMillis
}
