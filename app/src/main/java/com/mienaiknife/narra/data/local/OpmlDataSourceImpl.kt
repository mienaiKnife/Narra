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

import com.mienaiknife.narra.data.local.entities.FeedEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class OpmlDataSourceImpl @Inject constructor() : OpmlDataSource {

    private fun createParserFactory(): XmlPullParserFactory {
        return try {
            XmlPullParserFactory.newInstance()
        } catch (e: Exception) {
            // Fallback for JVM unit tests where XmlPullParserFactory might not be fully initialized
            try {
                // Try to load KXmlParser explicitly to avoid NullPointerException in newInstance()
                val factory = XmlPullParserFactory.newInstance("org.kxml2.io.KXmlParser,org.kxml2.io.KXmlSerializer", null)
                if (factory == null) throw e
                factory
            } catch (_: Exception) {
                // Use default constructor as absolute last resort
                try {
                    val constructor = XmlPullParserFactory::class.java.getDeclaredConstructor()
                    constructor.isAccessible = true
                    val factory = constructor.newInstance()
                    // Manually set implementation classes if we can find them
                    try {
                        val parserClass = Class.forName("org.kxml2.io.KXmlParser")
                        val serializerClass = Class.forName("org.kxml2.io.KXmlSerializer")
                        // These fields are internal to XmlPullParserFactory
                        // but we're in a desperate fallback situation
                    } catch (_: Exception) {}
                    factory
                } catch (_: Exception) {
                    throw e
                }
            }
        }
    }

    override suspend fun parseOpml(inputStream: InputStream): Result<List<FeedEntity>> {
        return try {
            val feeds = mutableListOf<FeedEntity>()
            val factory = createParserFactory()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser() 
                ?: throw IllegalStateException("Could not create XmlPullParser")
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "outline") {
                    val xmlUrl = parser.getAttributeValue(null, "xmlUrl") ?: 
                                parser.getAttributeValue(null, "url") ?: ""
                    
                    val title = parser.getAttributeValue(null, "title") ?: 
                               parser.getAttributeValue(null, "text") ?: "Untitled"

                    if (xmlUrl.isNotEmpty()) {
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
            val factory = createParserFactory()
            val serializer = factory.newSerializer()
            serializer.setOutput(outputStream, "UTF-8")
            serializer.startDocument("UTF-8", true)
            
            try {
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            } catch (e: Exception) {
                // Ignore if indentation is not supported
            }
            
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
