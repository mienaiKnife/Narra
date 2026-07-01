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
package com.mienaiknife.narra.data.models

import com.mienaiknife.narra.domain.models.Article

object SampleArticles {
    val sampleArticle1 =
        Article(
            id = "1",
            title = "Technically Radical: On The Unrecognized Potential of Tech Workers and Hackers",
            source = "We Don't Agree",
            publishedAt = "Aug 1, 2025",
            content =
            """
                <blockquote>They're so thoroughly hypnotized by the short-term victory of global capitalism that they can't surf the new paradigm, look to the longer term.<br/><br/>— Charles Stross, Accelerando</blockquote>
                <p>Features support check:</p>
                <ol>
                    <li>Ordered Item 1</li>
                    <li>Ordered Item 2</li>
                </ol>
                <p>Some <del>strikethrough text</del> here.</p>
                <table>
                    <tr><th>Feature</th><th>Status</th></tr>
                    <tr><td>Tables</td><td>Supported</td></tr>
                    <tr><td>Lists</td><td>Numbered</td></tr>
                </table>
                <p>Over 2024, we saw major tech investors and company owners overtly turn toward the right, overtly backing Donald Trump in his electoral campaign. This group of individuals, which commentators have taken to calling the “Tech Right”, are motivated by various concerns like the attempts by the Biden administration to regulate them and a general backlash against “wokeness”. Representative individuals and their respective justifications can be found in things like Marc Andreessen's essay The Techno-Optimist Manifesto or books like Balaji Srinivasan’s The Network State, and Alexander Karp and Nicholas Zamiska’s The Technological Republic.</p>
            """.trimIndent(),
            progress = 0.8f,
            isFavorite = false,
            isFromFeed = false,
            isInQueue = true,
        )

    val sampleArticle2 =
        Article(
            id = "2",
            title = "Modern Android Development",
            source = "Android Developers",
            publishedAt = "Mar 20, 2025",
            content = "Jetpack Compose is the modern toolkit for building native UI on Android...",
            progress = 0.5f,
            isFavorite = true,
            isFromFeed = true,
            isInQueue = true,
        )

    val sampleArticle3 =
        Article(
            id = "3",
            title = "The Future of RSS",
            source = "The Verge",
            publishedAt = "Feb 15, 2025",
            content = "Is the open web making a comeback? Some say RSS is the key to escaping algorithmic feeds...",
            isFavorite = false,
            isFromFeed = true,
            isInQueue = false,
        )

    val finishedArticle =
        Article(
            id = "4",
            title = "Sustainable Web Design",
            source = "A List Apart",
            publishedAt = "Jan 10, 2025",
            content = "",
            progress = 1.0f,
            isFavorite = true,
            isFromFeed = false,
            isInQueue = false,
        )

    val all = listOf(sampleArticle1, sampleArticle2, sampleArticle3, finishedArticle)
}
