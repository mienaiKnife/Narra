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
            assert(isInInbox == 1) // Should be marked as in Inbox based on the migration logic
            cursor.close()
        }
    }
}
