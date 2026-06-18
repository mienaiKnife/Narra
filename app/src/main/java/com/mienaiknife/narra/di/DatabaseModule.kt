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
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager,
    ): AppDatabase {
        System.loadLibrary("sqlcipher")
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val passphrase = securityManager.getDatabaseEncryptionKey()

        // 1. If DB exists, check if it's already encrypted
        if (dbFile.exists()) {
            val isEncrypted =
                try {
                    net.zetetic.database.sqlcipher.SQLiteDatabase
                        .openDatabase(
                            dbFile.absolutePath,
                            passphrase,
                            null,
                            net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                            null,
                        ).use { it.isOpen }
                } catch (e: Exception) {
                    false
                }

            if (!isEncrypted) {
                // 2. Try to open as unencrypted to confirm it's a migration case
                val isUnencrypted =
                    try {
                        net.zetetic.database.sqlcipher.SQLiteDatabase
                            .openDatabase(
                                dbFile.absolutePath,
                                "".toByteArray(),
                                null,
                                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                                null,
                            ).use { it.isOpen }
                    } catch (e: Exception) {
                        false
                    }

                if (isUnencrypted) {
                    android.util.Log.i("DatabaseModule", "Unencrypted database found. Encrypting...")
                    try {
                        encryptDatabase(context, dbFile, passphrase)
                        android.util.Log.i("DatabaseModule", "Database encrypted successfully.")
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseModule", "Failed to encrypt database", e)
                    }
                } else {
                    android.util.Log.e("DatabaseModule", "Database is corrupted or encrypted with a different key.")
                    dbFile.delete()
                    File(dbFile.path + "-wal").delete()
                    File(dbFile.path + "-shm").delete()
                }
            }
        }

        val factory = SupportOpenHelperFactory(passphrase)

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

        return Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME,
            ).openHelperFactory(factory)
            .addMigrations(migration16to17)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    private fun encryptDatabase(
        context: Context,
        dbFile: File,
        passphrase: ByteArray,
    ) {
        val tempDbFile = File(context.cacheDir, "temp_encrypt.db")
        if (tempDbFile.exists()) tempDbFile.delete()

        net.zetetic.database.sqlcipher.SQLiteDatabase
            .openDatabase(
                dbFile.absolutePath,
                "".toByteArray(),
                null,
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,
                null,
            ).use { db ->
                val passphraseHex = passphrase.joinToString("") { "%02x".format(it) }
                db.rawExecSQL("ATTACH DATABASE '${tempDbFile.absolutePath}' AS encrypted KEY x'$passphraseHex';")
                db.rawExecSQL("SELECT sqlcipher_export('encrypted');")
                db.rawExecSQL("DETACH DATABASE encrypted;")
            }

        dbFile.delete()
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        tempDbFile.renameTo(dbFile)
    }

    @Provides
    fun provideArticleDao(database: AppDatabase): ArticleDao = database.articleDao()

    @Provides
    fun provideFeedDao(database: AppDatabase): FeedDao = database.feedDao()

    @Provides
    fun provideTtsModelDao(database: AppDatabase): TtsModelDao = database.ttsModelDao()
}
