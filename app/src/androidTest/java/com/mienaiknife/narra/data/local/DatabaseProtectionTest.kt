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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mienaiknife.narra.di.DatabaseModule
import com.mienaiknife.narra.utils.SecurityManager
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
<<<<<<< HEAD
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
=======
import java.io.File
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
>>>>>>> origin/main

@RunWith(AndroidJUnit4::class)
class DatabaseProtectionTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private val passphrase = "test-passphrase".toByteArray()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
<<<<<<< HEAD

=======
        
>>>>>>> origin/main
        // Ensure parent directory exists
        dbFile.parentFile?.mkdirs()

        // Ensure clean state
        dbFile.delete()
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
<<<<<<< HEAD

        // Clean up any old backups
        context.databaseList().forEach {
=======
        
        // Clean up any old backups
        context.databaseList().forEach { 
>>>>>>> origin/main
            if (it.startsWith(AppDatabase.DATABASE_NAME + ".bak_")) {
                context.deleteDatabase(it)
            }
        }
    }

    @Test
    fun testUnencryptedToEncryptedMigration() {
        // 1. Create an unencrypted database with some data
        System.loadLibrary("sqlcipher")
        // Use CREATE_IF_NECESSARY | OPEN_READWRITE
        val flags = SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.OPEN_READWRITE
        SQLiteDatabase.openDatabase(dbFile.absolutePath, "", null, flags, null).use { db ->
            db.execSQL("CREATE TABLE test_table (id TEXT PRIMARY KEY, value TEXT)")
            db.execSQL("INSERT INTO test_table (id, value) VALUES ('1', 'unencrypted-data')")
        }

        // Verify it is unencrypted
        SQLiteDatabase.openDatabase(dbFile.absolutePath, "", null, SQLiteDatabase.OPEN_READONLY, null).use { db ->
            assertTrue("Database should be openable without passphrase", db.isOpen)
        }

        // 2. Mock SecurityManager to return our test passphrase
        val securityManager = mock<SecurityManager>()
        whenever(securityManager.getDatabaseEncryptionKey()).thenReturn(passphrase)

        // 3. Trigger DatabaseModule encryption logic
        android.util.Log.i("DatabaseProtectionTest", "Triggering provideAppDatabase for encryption test")
        DatabaseModule.provideAppDatabase(context, securityManager)
        android.util.Log.i("DatabaseProtectionTest", "provideAppDatabase finished")

        // 4. Verify it is now encrypted
        val canOpenWithoutPassphrase = try {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, "", null, SQLiteDatabase.OPEN_READONLY, null).use { it.isOpen }
        } catch (e: Exception) {
            false
        }
        assertFalse("Database should NOT be openable without passphrase", canOpenWithoutPassphrase)

        // 5. Verify it can be opened with the passphrase and data is preserved
        SQLiteDatabase.openDatabase(dbFile.absolutePath, passphrase, null, SQLiteDatabase.OPEN_READONLY, null).use { db ->
            assertTrue("Database should be openable with correct passphrase", db.isOpen)
            val cursor = db.rawQuery("SELECT value FROM test_table WHERE id = '1'", null)
            assertTrue("Cursor should have data", cursor.moveToFirst())
            assertTrue("Data should match original", cursor.getString(0) == "unencrypted-data")
            cursor.close()
        }
    }

    @Test
    fun testKeyMismatchCorruptionProtection() {
        // 1. Create an encrypted database with an OLD key
        val oldPassphrase = "old-passphrase".toByteArray()
        System.loadLibrary("sqlcipher")
        val flags = SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.OPEN_READWRITE
        SQLiteDatabase.openDatabase(dbFile.absolutePath, oldPassphrase, null, flags, null).use { db ->
            db.execSQL("CREATE TABLE test_table (id TEXT PRIMARY KEY, value TEXT)")
            db.execSQL("INSERT INTO test_table (id, value) VALUES ('1', 'old-secret-data')")
        }

        // 2. Mock SecurityManager to return a NEW (different) key
        val newPassphrase = "new-passphrase".toByteArray()
        val securityManager = mock<SecurityManager>()
        whenever(securityManager.getDatabaseEncryptionKey()).thenReturn(newPassphrase)

        // 3. Trigger DatabaseModule logic
        DatabaseModule.provideAppDatabase(context, securityManager)

        // 4. Verify that a backup was created
        val backups = context.databaseList().filter { it.startsWith(AppDatabase.DATABASE_NAME + ".bak_") }
        assertTrue("At least one backup file should exist", backups.isNotEmpty())

        // 5. Verify the NEW database is fresh (empty or schema-only, not containing old data)
        SQLiteDatabase.openDatabase(dbFile.absolutePath, newPassphrase, null, SQLiteDatabase.OPEN_READONLY, null).use { db ->
            assertTrue("New database should be openable with new passphrase", db.isOpen)
<<<<<<< HEAD

=======
            
>>>>>>> origin/main
            // Check if test_table exists
            val tableExists = try {
                db.rawQuery("SELECT 1 FROM test_table", null).use { it.moveToFirst() }
            } catch (e: Exception) {
                false
            }
            assertFalse("Old table should not exist in the new database", tableExists)
        }
    }
}
