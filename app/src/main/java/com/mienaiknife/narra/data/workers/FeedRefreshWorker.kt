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
package com.mienaiknife.narra.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mienaiknife.narra.domain.repository.ContentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FeedRefreshWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val contentRepository: ContentRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val result = contentRepository.refreshFeeds()
        if (result.isSuccess) {
            Result.success()
        } else {
            android.util.Log.e("FeedRefreshWorker", "Feed refresh failed: ${result.exceptionOrNull()?.message}")
            Result.retry()
        }
    } catch (e: Exception) {
        android.util.Log.e("FeedRefreshWorker", "Exception in FeedRefreshWorker", e)
        Result.retry()
    }
}
