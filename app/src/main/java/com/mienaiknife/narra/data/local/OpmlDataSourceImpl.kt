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
import org.w3c.dom.Element
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class OpmlDataSourceImpl @Inject constructor() : OpmlDataSource {

    override suspend fun parseOpml(inputStream: InputStream): Result<List<FeedEntity>> {
        return try {
            val feeds = mutableListOf<FeedEntity>()
            val factory = DocumentBuilderFactory.newInstance()
            
            // Disable XXE
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)

            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(inputStream)
            
            val outlines = doc.getElementsByTagName("outline")
            for (i in 0 until outlines.length) {
                val node = outlines.item(i) as Element
                val xmlUrl = node.getAttribute("xmlUrl")
                val title = if (node.hasAttribute("title")) {
                    node.getAttribute("title")
                } else if (node.hasAttribute("text")) {
                    node.getAttribute("text")
                } else {
                    "Untitled"
                }
                
                if (xmlUrl.isNotEmpty()) {
                    feeds.add(
                        FeedEntity(
                            url = xmlUrl,
                            title = title
                        )
                    )
                }
            }
            Result.success(feeds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateOpml(outputStream: OutputStream, feeds: List<FeedEntity>): Result<Unit> {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.newDocument()
            
            val opml = doc.createElement("opml")
            opml.setAttribute("version", "2.0")
            doc.appendChild(opml)
            
            val head = doc.createElement("head")
            val title = doc.createElement("title")
            title.appendChild(doc.createTextNode("Narra Subscriptions"))
            head.appendChild(title)
            opml.appendChild(head)
            
            val body = doc.createElement("body")
            for (feed in feeds) {
                val outline = doc.createElement("outline")
                outline.setAttribute("type", "rss")
                outline.setAttribute("text", feed.title)
                outline.setAttribute("title", feed.title)
                outline.setAttribute("xmlUrl", feed.url)
                body.appendChild(outline)
            }
            opml.appendChild(body)
            
            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            
            val source = DOMSource(doc)
            val result = StreamResult(outputStream)
            transformer.transform(source, result)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
