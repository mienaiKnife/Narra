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
package com.mienaiknife.narra.utils

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.FeedEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class NotificationHelperTest {
    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    @Test
    fun `creates the feed notification channel`() {
        NotificationHelper(context)

        val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID)

        assertNotNull("Feed notification channel should be created", channel)
    }

    @Test
    fun `posts a notification for a new article when permission is granted`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val feed = FeedEntity(url = "feed", title = "Example Feed")
        val article = ArticleEntity(id = "a1", title = "Hello World", source = "test", content = null)

        NotificationHelper(context).showNewArticleNotification(feed, article)

        val posted = notificationManager.activeNotifications
        assertEquals(1, posted.size)
        val shadowNotification = shadowOf(posted.first().notification)
        assertEquals(
            context.getString(R.string.notification_new_article_title, feed.title),
            shadowNotification.contentTitle.toString(),
        )
        assertEquals(article.title, shadowNotification.contentText.toString())
    }

    @Test
    fun `does not post a notification when permission is denied`() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val feed = FeedEntity(url = "feed", title = "Example Feed")
        val article = ArticleEntity(id = "a1", title = "Hello World", source = "test", content = null)

        NotificationHelper(context).showNewArticleNotification(feed, article)

        assertEquals(0, notificationManager.activeNotifications.size)
    }
}
