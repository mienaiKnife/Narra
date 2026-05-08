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

package com.mienaiknife.narra.di

import com.mienaiknife.narra.data.local.EpubDataSource
import com.mienaiknife.narra.data.local.EpubDataSourceImpl
import com.mienaiknife.narra.data.local.ImageDataSource
import com.mienaiknife.narra.data.local.ImageDataSourceImpl
import com.mienaiknife.narra.data.local.OpmlDataSource
import com.mienaiknife.narra.data.local.OpmlDataSourceImpl
import com.mienaiknife.narra.data.local.dao.ArticleDao
import com.mienaiknife.narra.data.local.dao.FeedDao
import com.mienaiknife.narra.data.remote.RemoteFeedDataSource
import com.mienaiknife.narra.data.remote.RemoteFeedDataSourceImpl
import com.mienaiknife.narra.data.remote.WebDataSource
import com.mienaiknife.narra.data.remote.WebDataSourceImpl
import com.mienaiknife.narra.data.repositories.ContentRepositoryImpl
import com.mienaiknife.narra.data.repositories.ModelRepositoryImpl
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.ui.utils.NetworkMonitor
import com.mienaiknife.narra.ui.utils.NetworkMonitorImpl
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideRssParser(): RssParser {
        return RssParserBuilder().build()
    }

    @Provides
    @Singleton
    fun provideContentRepository(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        database: com.mienaiknife.narra.data.local.AppDatabase,
        articleDao: ArticleDao,
        feedDao: FeedDao,
        webDataSource: WebDataSource,
        remoteFeedDataSource: RemoteFeedDataSource,
        epubDataSource: EpubDataSource,
        opmlDataSource: OpmlDataSource,
        networkMonitor: NetworkMonitor,
        downloadSettingsManager: DownloadSettingsManager,
        imageDataSource: ImageDataSource,
        notificationHelper: com.mienaiknife.narra.utils.NotificationHelper
    ): ContentRepository {
        return ContentRepositoryImpl(
            context,
            database,
            articleDao,
            feedDao,
            webDataSource,
            remoteFeedDataSource,
            epubDataSource,
            imageDataSource,
            opmlDataSource,
            networkMonitor,
            downloadSettingsManager,
            notificationHelper
        )
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): com.mienaiknife.narra.utils.NotificationHelper {
        return com.mienaiknife.narra.utils.NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideImageDataSource(imageDataSourceImpl: ImageDataSourceImpl): ImageDataSource = imageDataSourceImpl

    @Provides
    @Singleton
    fun provideModelRepository(modelRepositoryImpl: ModelRepositoryImpl): ModelRepository = modelRepositoryImpl

    @Provides
    @Singleton
    fun provideWebDataSource(webDataSourceImpl: WebDataSourceImpl): WebDataSource = webDataSourceImpl

    @Provides
    @Singleton
    fun provideRemoteFeedDataSource(remoteFeedDataSourceImpl: RemoteFeedDataSourceImpl): RemoteFeedDataSource = remoteFeedDataSourceImpl

    @Provides
    @Singleton
    fun provideEpubDataSource(epubDataSourceImpl: EpubDataSourceImpl): EpubDataSource = epubDataSourceImpl

    @Provides
    @Singleton
    fun provideOpmlDataSource(opmlDataSourceImpl: OpmlDataSourceImpl): OpmlDataSource = opmlDataSourceImpl

    @Provides
    @Singleton
    fun provideNetworkMonitor(networkMonitorImpl: NetworkMonitorImpl): NetworkMonitor {
        return networkMonitorImpl
    }
}
