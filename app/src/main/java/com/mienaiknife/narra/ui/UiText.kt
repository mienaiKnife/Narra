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
package com.mienaiknife.narra.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.NarraError

sealed class UiText {
    data class DynamicString(
        val value: String,
    ) : UiText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any,
    ) : UiText()

    class PluralResource(
        @PluralsRes val resId: Int,
        val count: Int,
        vararg val args: Any,
    ) : UiText()

    companion object {
        fun fromError(error: Throwable): UiText = when (error) {
            is NarraError.Network.NoConnection -> StringResource(R.string.error_no_internet)
            is NarraError.Content.ArticleAlreadyInQueue -> StringResource(R.string.error_article_already_in_queue)
            is NarraError.Content.NotFound -> StringResource(R.string.error_article_not_found)
            is NarraError.Content.InvalidFeed -> StringResource(R.string.error_subscribe_failed)
            is NarraError.Network.WifiRequired -> StringResource(R.string.settings_downloads_wifi_only) // Using existing string for now
            is NarraError.Model.DownloadFailed -> StringResource(R.string.error_download_failed)
            is NarraError.Content.ParsingFailed -> StringResource(R.string.error_generic)
            else -> error.message?.let { DynamicString(it) } ?: StringResource(R.string.error_generic)
        }
    }

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(resId, *args)
        is PluralResource -> pluralStringResource(resId, count, *args)
    }

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(resId, *args)
        is PluralResource -> context.resources.getQuantityString(resId, count, *args)
    }
}
