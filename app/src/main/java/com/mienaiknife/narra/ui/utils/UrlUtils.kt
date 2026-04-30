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

import androidx.core.net.toUri
import java.net.InetAddress
import java.net.URL

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
            val uri = url.toUri()
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
        } catch (_: Exception) {
            url
        }
    }

    fun getDomainName(url: String): String {
        return try {
            val uri = URL(url)
            val host = uri.host
            if (host.startsWith("www.")) {
                host.substring(4)
            } else {
                host
            }
        } catch (_: Exception) {
            url
        }
    }

    /**
     * Checks if the given URL is a public URL and not a local/private network address.
     * This is a basic SSRF protection measure.
     */
    fun isPublicUrl(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val host = url.host.lowercase()

            // 1. Basic check for localhost and local names
            if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host.endsWith(".local")) {
                return false
            }

            // 2. Deep check for IP ranges (requires network lookup or careful pattern matching)
            // For SSRF, we want to avoid 10.x.x.x, 192.168.x.x, 172.16.x.x, etc.
            // Note: In a real app, you'd might want to do this asynchronously to avoid blocking
            // but for simple UI checks or before passing to Jsoup, it's often done synchronously.
            val address = InetAddress.getByName(host)
            !(address.isLoopbackAddress || address.isAnyLocalAddress || address.isLinkLocalAddress || address.isSiteLocalAddress)
        } catch (_: Exception) {
            // If we can't parse it or resolve it, we don't treat it as a safe public URL
            false
        }
    }
}
