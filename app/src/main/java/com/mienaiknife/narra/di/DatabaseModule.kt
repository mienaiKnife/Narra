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

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.mienaiknife.narra.data.local.AppDatabase
import com.mienaiknife.narra.data.local.dao.ArticleDao
import com.mienaiknife.narra.data.local.dao.FeedDao
import com.mienaiknife.narra.data.local.dao.TtsModelDao
import com.mienaiknife.narra.utils.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    val migration16to17 =
        object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Check if column exists before adding it (idempotency fix)
                val cursor = db.query("PRAGMA table_info(articles)")
                var columnExists = false
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (name == "isInInbox") {
                            columnExists = true
                            break
                        }
                    }
                }
                cursor.close()

                if (!columnExists) {
                    db.execSQL("ALTER TABLE articles ADD COLUMN isInInbox INTEGER NOT NULL DEFAULT 0")
                }

                db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_isInInbox_sortTimestamp ON articles(isInInbox, sortTimestamp)")

                // For existing feed articles, mark them as in Inbox if they are not in queue and not played
                db.execSQL(
                    "UPDATE articles SET isInInbox = 1 WHERE isFromFeed = 1 AND isInQueue = 0 AND progress < 1.0 AND finishedAt IS NULL",
                )
            }
        }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager,
    ): AppDatabase = Room
        .databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        ).openHelperFactory(PreparingOpenHelperFactory(securityManager))
        .addMigrations(migration16to17)
        // Schemas before 16 were never released and their Room schemas were not exported, so no
        // faithful migration path exists. Rather than crashing on `IllegalStateException`, wipe
        // and recreate the database for those legacy development installs. Real migrations must
        // still be provided for every schema from 16 onwards.
        .fallbackToDestructiveMigrationFrom(true, *LEGACY_UNSUPPORTED_SCHEMA_VERSIONS)
        .build()

    /**
     * Versions below [AppDatabase.MIN_SUPPORTED_VERSION] predate schema export and cannot be
     * migrated losslessly; allow Room to recreate the database for them.
     */
    private val LEGACY_UNSUPPORTED_SCHEMA_VERSIONS =
        (1 until AppDatabase.MIN_SUPPORTED_VERSION).toList().toIntArray()

    /**
     * Validates and repairs the on-disk database before it is opened. This reads the Keystore and
     * may encrypt or back up the whole database file, so it must never run on the main thread.
     * [PreparingOpenHelperFactory] invokes it lazily just before the first open, which Room
     * performs on a background thread for suspend DAO calls.
     */
    fun prepareDatabaseFile(
        context: Context,
        passphrase: ByteArray,
    ) {
        System.loadLibrary("sqlcipher")
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)

        if (!dbFile.exists()) {
            android.util.Log.i("DatabaseModule", "No database file found. Creating new one.")
            return
        }

        // 1. If DB exists, check if it's already encrypted
        android.util.Log.i("DatabaseModule", "Database file exists at: ${dbFile.absolutePath}")
        val isEncrypted =
            try {
                net.zetetic.database.sqlcipher.SQLiteDatabase
                    .openDatabase(
                        dbFile.absolutePath,
                        passphrase,
                        null,
                        net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                        null,
                    ).use { it.hasReadableSchema() }
            } catch (e: Exception) {
                android.util.Log.w("DatabaseModule", "Failed to open encrypted database: ${e.message}")
                false
            }

        if (isEncrypted) {
            android.util.Log.i("DatabaseModule", "Database opened successfully with current passphrase.")
            return
        }

        // 2. Try to open as unencrypted to confirm it's a migration case
        val isUnencrypted =
            try {
                net.zetetic.database.sqlcipher.SQLiteDatabase
                    .openDatabase(
                        dbFile.absolutePath,
                        "",
                        null,
                        net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                        null,
                    ).use { it.hasReadableSchema() }
            } catch (e: Exception) {
                android.util.Log.w("DatabaseModule", "Failed to open unencrypted database: ${e.message}")
                false
            }

        if (isUnencrypted) {
            android.util.Log.i("DatabaseModule", "Unencrypted database found. Encrypting...")
            try {
                encryptDatabase(dbFile, passphrase)
                android.util.Log.i("DatabaseModule", "Database encrypted successfully.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseModule", "Failed to encrypt database", e)
                // This might happen if encryption fails mid-way, or file is weirdly formatted
                backupAndStartFresh(dbFile)
            }
        } else {
            android.util.Log.e("DatabaseModule", "Database is corrupted, encrypted with a DIFFERENT key, or inaccessible. BACKING UP and starting fresh.")
            backupAndStartFresh(dbFile)
        }
    }

    private class PreparingOpenHelperFactory(
        private val securityManager: SecurityManager,
    ) : SupportSQLiteOpenHelper.Factory {
        override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper = PreparingOpenHelper(configuration, securityManager)
    }

    /**
     * Defers all SQLCipher/Keystore work until the database is actually opened. Room calls
     * [create] and [setWriteAheadLoggingEnabled] on the main thread during initialization, but
     * only touches [writableDatabase]/[readableDatabase] on the background query thread.
     */
    private class PreparingOpenHelper(
        private val configuration: SupportSQLiteOpenHelper.Configuration,
        private val securityManager: SecurityManager,
    ) : SupportSQLiteOpenHelper {
        private var delegate: SupportSQLiteOpenHelper? = null
        private var writeAheadLoggingEnabled: Boolean? = null

        @Synchronized
        private fun delegate(): SupportSQLiteOpenHelper {
            delegate?.let { return it }
            val passphrase = securityManager.getDatabaseEncryptionKey()
            prepareDatabaseFile(configuration.context, passphrase)
            return SupportOpenHelperFactory(passphrase)
                .create(configuration)
                .also { created ->
                    writeAheadLoggingEnabled?.let(created::setWriteAheadLoggingEnabled)
                    delegate = created
                }
        }

        override val databaseName: String?
            get() = configuration.name

        @Synchronized
        override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
            writeAheadLoggingEnabled = enabled
            delegate?.setWriteAheadLoggingEnabled(enabled)
        }

        override val writableDatabase: SupportSQLiteDatabase
            get() = delegate().writableDatabase

        override val readableDatabase: SupportSQLiteDatabase
            get() = delegate().readableDatabase

        @Synchronized
        override fun close() {
            delegate?.close()
        }
    }

    /**
     * SQLCipher opens the database lazily, so [SQLiteDatabase.isOpen] can be `true` even when the
     * provided key is wrong. Reading a page forces header/key validation and throws on mismatch.
     */
    private fun net.zetetic.database.sqlcipher.SQLiteDatabase.hasReadableSchema(): Boolean = rawQuery("SELECT count(*) FROM sqlite_schema", null).use { it.moveToFirst() }

    private fun backupAndStartFresh(dbFile: File) {
        val timestamp = System.currentTimeMillis()
        val bakFile = File(dbFile.path + ".bak_$timestamp")
        dbFile.renameTo(bakFile)
        File(dbFile.path + "-wal").let { if (it.exists()) it.renameTo(File(it.path + ".bak_$timestamp-wal")) }
        File(dbFile.path + "-shm").let { if (it.exists()) it.renameTo(File(it.path + ".bak_$timestamp-shm")) }
    }

    private fun encryptDatabase(
        dbFile: File,
        passphrase: ByteArray,
    ) {
        val tempDbFile = File(dbFile.parentFile, "temp_encrypt.db")
        if (tempDbFile.exists()) tempDbFile.delete()
        tempDbFile.parentFile?.mkdirs()
        tempDbFile.createNewFile()

        net.zetetic.database.sqlcipher.SQLiteDatabase
            .openDatabase(
                dbFile.absolutePath,
                "",
                null,
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,
                null,
            ).use { db ->
                val passphraseHex = passphrase.joinToString("") { "%02x".format(it) }
                // ATTACH does not support bound parameters, so the path must be escaped.
                val escapedTempPath = tempDbFile.absolutePath.replace("'", "''")
                db.rawExecSQL("ATTACH DATABASE '$escapedTempPath' AS encrypted KEY x'$passphraseHex';")
                db.rawExecSQL("SELECT sqlcipher_export('encrypted');")
                db.rawExecSQL("DETACH DATABASE encrypted;")
            }

        // Verify temp file exists and has content before replacing.
        if (!tempDbFile.exists() || tempDbFile.length() == 0L) {
            throw IllegalStateException("Encryption failed: temporary database is empty or missing")
        }

        // Swap atomically and reversibly: move the original aside, then move the
        // encrypted copy into place. If anything fails, restore the original.
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        val backupFile = File(dbFile.path + ".old")
        if (backupFile.exists()) backupFile.delete()

        if (dbFile.exists() && !dbFile.renameTo(backupFile)) {
            throw IllegalStateException("Encryption failed: could not back up original database")
        }

        try {
            // Sidecars belong to the plaintext database and must not survive the swap.
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            // renameTo is atomic for paths on the same filesystem and works on all API levels.
            if (!tempDbFile.renameTo(dbFile)) {
                throw IllegalStateException("Encryption failed: could not move encrypted database into place")
            }
        } catch (e: Exception) {
            if (dbFile.exists()) dbFile.delete()
            if (!backupFile.renameTo(dbFile)) {
                android.util.Log.e("DatabaseModule", "Failed to restore original database after encryption failure", e)
            }
            throw e
        }

        backupFile.delete()
    }

    @Provides
    fun provideArticleDao(database: AppDatabase): ArticleDao = database.articleDao()

    @Provides
    fun provideFeedDao(database: AppDatabase): FeedDao = database.feedDao()

    @Provides
    fun provideTtsModelDao(database: AppDatabase): TtsModelDao = database.ttsModelDao()
}
