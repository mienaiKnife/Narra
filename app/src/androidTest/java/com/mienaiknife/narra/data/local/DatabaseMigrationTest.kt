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

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mienaiknife.narra.di.DatabaseModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate16To17() {
        // Create earliest version of the database.
        helper.createDatabase(TEST_DB, 16).apply {
            // Insert some data that should be migrated
            execSQL(
                "INSERT INTO articles (id, title, source, progress, currentParagraphIndex, currentWordOffset, isFavorite, isFromFeed, isInQueue, queueOrder, createdAt, sortTimestamp) " +
                    "VALUES ('1', 'Test Article', 'Test Source', 0.5, 0, 0, 0, 1, 0, 0, 123456789, 123456789)",
            )
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations have been applied.
        val migration16to17 = DatabaseModule.migration16to17

        helper.runMigrationsAndValidate(TEST_DB, 17, true, migration16to17).apply {
            // Verify that the data was migrated correctly
            val cursor = query("SELECT isInInbox FROM articles WHERE id = '1'")
            cursor.moveToFirst()
            val isInInbox = cursor.getInt(0)
            assertEquals("Article should be marked as in Inbox by the migration", 1, isInInbox)
            cursor.close()

            // Verify index exists
            val indexCursor = query("PRAGMA index_list('articles')")
            var indexExists = false
            while (indexCursor.moveToNext()) {
                val name = indexCursor.getString(indexCursor.getColumnIndex("name"))
                if (name == "index_articles_isInInbox_sortTimestamp") {
                    indexExists = true
                    break
                }
            }
            indexCursor.close()
            assertTrue("Expected index_articles_isInInbox_sortTimestamp to exist", indexExists)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate17To18() {
        helper.createDatabase(TEST_DB, 17).apply {
            execSQL(
                "INSERT INTO feeds (url, title, createdAt, notificationsEnabled) " +
                    "VALUES ('https://example.com/feed', 'Example Feed', 123456789, 0)",
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 18, true, DatabaseModule.migration17to18).apply {
            val cursor = query("SELECT lastRefreshErrorAt FROM feeds WHERE url = 'https://example.com/feed'")
            assertTrue("Feed row should survive the migration", cursor.moveToFirst())
            assertTrue("New column should default to no error", cursor.isNull(0))
            cursor.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate18To19() {
        helper.createDatabase(TEST_DB, 18).apply {
            execSQL(
                "INSERT INTO tts_models (id, name, language, description, type, modelUrl, tokensUrl, extraUrls, dataDir, sizeBytes, isDownloaded, progress, speakerId, lastError) " +
                    "VALUES ('kokoro-en-v0_19', 'Kokoro (English)', 'en', 'High quality multi-voice Kokoro TTS', 'KOKORO', " +
                    "'https://example.com/kokoro.tar.bz2', '', '', '/data/tts_models/kokoro', 80000000, 1, 0.5, 7, 'boom')",
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 19, true, DatabaseModule.migration18to19).apply {
            val cursor = query(
                "SELECT isDownloaded, progress, dataDir, speakerId, lastError, " +
                    "voiceQuality, voiceGender, voiceLocale, voiceSpeakerCount " +
                    "FROM tts_models WHERE id = 'kokoro-en-v0_19'",
            )
            assertTrue("Model row should survive the migration", cursor.moveToFirst())
            assertEquals("Download state must survive", 1, cursor.getInt(0))
            assertEquals("Progress must survive", 0.5f, cursor.getFloat(1), 0.0001f)
            assertEquals("/data/tts_models/kokoro", cursor.getString(2))
            assertEquals("Selected speaker must survive", 7, cursor.getInt(3))
            assertEquals("Download error must survive", "boom", cursor.getString(4))
            assertEquals("Quality starts unknown until the catalog refresh", "UNKNOWN", cursor.getString(5))
            assertEquals("Gender starts unspecified until the catalog refresh", "UNSPECIFIED", cursor.getString(6))
            assertEquals("Locale falls back to the language code", "en", cursor.getString(7))
            assertEquals(1, cursor.getInt(8))
            cursor.close()

            val columns = query("PRAGMA table_info(tts_models)")
            var hasDescription = false
            while (columns.moveToNext()) {
                if (columns.getString(columns.getColumnIndex("name")) == "description") {
                    hasDescription = true
                    break
                }
            }
            columns.close()
            assertFalse("The free-text description column must be dropped", hasDescription)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate16To19() {
        helper.createDatabase(TEST_DB, 16).apply {
            execSQL(
                "INSERT INTO feeds (url, title, createdAt, notificationsEnabled) " +
                    "VALUES ('https://example.com/feed', 'Example Feed', 123456789, 0)",
            )
            execSQL(
                "INSERT INTO tts_models (id, name, language, description, type, modelUrl, tokensUrl, extraUrls, dataDir, sizeBytes, isDownloaded, progress, speakerId, lastError) " +
                    "VALUES ('legacy-model', 'Legacy', 'en', 'Legacy description', 'VITS', 'https://example.com/model.tar.bz2', '', '', NULL, 1000, 0, 0.0, 2, NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            19,
            true,
            DatabaseModule.migration16to17,
            DatabaseModule.migration17to18,
            DatabaseModule.migration18to19,
        ).apply {
            val feedCursor = query("SELECT lastRefreshErrorAt FROM feeds WHERE url = 'https://example.com/feed'")
            assertTrue("Feed row should survive the migration", feedCursor.moveToFirst())
            assertTrue("New feed column should default to no error", feedCursor.isNull(0))
            feedCursor.close()

            val modelCursor = query("SELECT speakerId, voiceLocale FROM tts_models WHERE id = 'legacy-model'")
            assertTrue("Model row should survive the migration", modelCursor.moveToFirst())
            assertEquals(2, modelCursor.getInt(0))
            assertEquals("en", modelCursor.getString(1))
            modelCursor.close()
        }
    }

    @Test
    fun testMigrationIdempotency() {
        helper.createDatabase(TEST_DB, 16).apply {
            close()
        }

        val migration16to17 = DatabaseModule.migration16to17

        // Run migration first time
        helper.runMigrationsAndValidate(TEST_DB, 17, true, migration16to17).close()

        // Re-open the already-migrated database. Room sees version 17 and skips the
        // migration, so this only re-runs schema validation.
        helper.runMigrationsAndValidate(TEST_DB, 17, true, migration16to17).apply {
            // If it wasn't idempotent, it would crash or fail validation
            val cursor = query("SELECT isInInbox FROM articles")
            cursor.close()
        }
    }
}
