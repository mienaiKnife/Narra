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

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.mienaiknife.narra.service.PlaybackService

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
        android.util.Log.d("PlaybackActionCallback", "onAction: action=$action")

        val intent =
            Intent(context, PlaybackService::class.java).apply {
                this.action = action
            }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaybackActionCallback", "Error starting service for action", e)
        }
    }
}

// Removed unused await extension
