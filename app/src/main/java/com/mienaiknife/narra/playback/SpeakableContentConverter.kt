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
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.models.SpeakableText
import com.mienaiknife.narra.ui.models.ContentBlock
import com.mienaiknife.narra.ui.utils.toSpeakableText

object SpeakableContentConverter {
    fun convert(
        context: Context,
        blocks: List<ContentBlock>,
        readAltText: Boolean,
        shortenHyperlinks: Boolean,
    ): List<SpeakableText> {
        return blocks.map { block ->
            if (block is ContentBlock.Image) {
                if (readAltText) {
                    val alt = block.altText?.let { context.getString(R.string.reader_image_prefix, it) } ?: ""
                    SpeakableText(alt)
                } else {
                    SpeakableText("")
                }
            } else {
                block.text.toSpeakableText(context, shortenLinks = shortenHyperlinks)
            }
        }
    }
}
