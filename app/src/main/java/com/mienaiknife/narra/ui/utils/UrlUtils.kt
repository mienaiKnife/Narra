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

package com.mienaiknife.narra.ui.utils

import android.net.Uri

object UrlUtils {
    private val TRACKING_PARAMS = listOf(
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "utm_id",
        "utm_source_platform",
        "utm_creative_format",
        "utm_marketing_tactic",
        "gclid",
        "fbclid",
        "mc_cid",
        "mc_eid",
        "_hsenc",
        "_hsmi",
        "hsCtaTracking",
        "mkt_tok",
        "igshid",
        "ref",
        "ref_src",
        "ref_url",
        "clickid",
        "irclickid",
        "msclkid",
        "tt_content",
        "tt_medium"
    )

    fun cleanUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            if (uri.isOpaque) return url

            val builder = uri.buildUpon().clearQuery()
            
            uri.queryParameterNames.forEach { name ->
                if (!TRACKING_PARAMS.contains(name.lowercase())) {
                    uri.getQueryParameters(name).forEach { value ->
                        builder.appendQueryParameter(name, value)
                    }
                }
            }
            
            builder.build().toString()
        } catch (e: Exception) {
            url
        }
    }
}
