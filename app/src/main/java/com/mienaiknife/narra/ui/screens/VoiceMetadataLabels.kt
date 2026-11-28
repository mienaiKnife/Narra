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
package com.mienaiknife.narra.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.models.VoiceGender
import com.mienaiknife.narra.domain.models.VoiceProfile
import com.mienaiknife.narra.domain.models.VoiceQuality
import java.util.Locale

/**
 * Renders [VoiceProfile] as one localized metadata line, for example
 * "Low quality · American English · Female". The catalog never stores this copy as free text, so
 * every voice reads the same way and follows the app language.
 */
@Composable
fun voiceProfileDescription(profile: VoiceProfile): String {
    val quality = when (profile.quality) {
        VoiceQuality.LOW -> stringResource(R.string.settings_voices_quality_low)
        VoiceQuality.MEDIUM -> stringResource(R.string.settings_voices_quality_medium)
        VoiceQuality.HIGH -> stringResource(R.string.settings_voices_quality_high)
        VoiceQuality.UNKNOWN -> null
    }

    val accent = localeAccentLabel(profile.locale)

    val voices = when {
        profile.speakerCount > 1 -> pluralStringResource(
            R.plurals.settings_voices_speaker_count,
            profile.speakerCount,
            profile.speakerCount,
        )

        profile.gender == VoiceGender.FEMALE -> stringResource(R.string.settings_voices_gender_female)
        profile.gender == VoiceGender.MALE -> stringResource(R.string.settings_voices_gender_male)
        profile.gender == VoiceGender.MIXED -> stringResource(R.string.settings_voices_gender_mixed)
        else -> null
    }

    return listOfNotNull(quality, accent, voices)
        .joinToString(stringResource(R.string.settings_voices_metadata_separator))
}

/**
 * Maps the BCP-47 [tag] to the accent name shown in the voice list. Known accents get a dedicated,
 * translatable name; every other tag falls back to the locale's own display name.
 */
@Composable
private fun localeAccentLabel(tag: String): String? {
    if (tag.isBlank()) return null
    val locale = Locale.forLanguageTag(tag)
    if (locale.language.isEmpty()) return tag
    // LocalConfiguration is observable, so the label follows a locale change.
    val appLocale = LocalConfiguration.current.locales[0]
    return when (locale.language to locale.country) {
        "en" to "US" -> stringResource(R.string.settings_voices_accent_american_english)
        else -> locale.getDisplayName(appLocale)
    }
}
