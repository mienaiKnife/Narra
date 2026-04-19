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

package com.mienaiknife.narra.data.local

import android.util.Xml
import com.mienaiknife.narra.data.local.entities.FeedEntity
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class OpmlDataSourceImpl @Inject constructor() : OpmlDataSource {

    override suspend fun parseOpml(inputStream: InputStream): Result<List<FeedEntity>> {
        return try {
            val feeds = mutableListOf<FeedEntity>()
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "outline") {
                    val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                    val title = parser.getAttributeValue(null, "title") 
                        ?: parser.getAttributeValue(null, "text")
                        ?: "Untitled"
                    
                    if (xmlUrl != null) {
                        feeds.add(
                            FeedEntity(
                                url = xmlUrl,
                                title = title
                            )
                        )
                    }
                }
                eventType = parser.next()
            }
            Result.success(feeds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateOpml(outputStream: OutputStream, feeds: List<FeedEntity>): Result<Unit> {
        return try {
            val serializer = Xml.newSerializer()
            serializer.setOutput(outputStream, "UTF-8")
            serializer.startDocument("UTF-8", true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            
            serializer.startTag(null, "opml")
            serializer.attribute(null, "version", "2.0")
            
            serializer.startTag(null, "head")
            serializer.startTag(null, "title")
            serializer.text("Narra Subscriptions")
            serializer.endTag(null, "title")
            serializer.endTag(null, "head")
            
            serializer.startTag(null, "body")
            for (feed in feeds) {
                serializer.startTag(null, "outline")
                serializer.attribute(null, "type", "rss")
                serializer.attribute(null, "text", feed.title)
                serializer.attribute(null, "title", feed.title)
                serializer.attribute(null, "xmlUrl", feed.url)
                serializer.endTag(null, "outline")
            }
            serializer.endTag(null, "body")
            
            serializer.endTag(null, "opml")
            serializer.endDocument()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
