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

package com.mienaiknife.narra.ui.widget

import android.content.ComponentName
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mienaiknife.narra.service.PlaybackService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PlaybackActionCallback : ActionCallback {
    companion object {
        private val KEY_ACTION = ActionParameters.Key<String>("action")
        const val ACTION_TOGGLE = "toggle"
        const val ACTION_SKIP_FORWARD = "skip_forward"
        const val ACTION_SKIP_BACKWARD = "skip_backward"
        const val ACTION_SKIP_NEXT = "skip_next"

        fun createParameters(action: String) = androidx.glance.action.actionParametersOf(KEY_ACTION to action)
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val action = parameters[KEY_ACTION] ?: ACTION_TOGGLE
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        try {
            val controller = controllerFuture.await()
            when (action) {
                ACTION_TOGGLE -> {
                    if (controller.isPlaying) controller.pause() else controller.play()
                }
                ACTION_SKIP_FORWARD -> controller.seekForward()
                ACTION_SKIP_BACKWARD -> controller.seekBack()
                ACTION_SKIP_NEXT -> controller.seekToNext()
            }
            controller.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
    addListener(
        {
            try {
                cont.resume(get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        },
        MoreExecutors.directExecutor(),
    )
    cont.invokeOnCancellation {
        cancel(false)
    }
}
