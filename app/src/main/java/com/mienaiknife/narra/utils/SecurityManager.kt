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

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val SECURE_PREFS_NAME = "secure_settings"
        private const val DB_ENCRYPTION_KEY = "db_encryption_key"
    }

    private val secureRandom = SecureRandom()

    private val masterKey by lazy {
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            // If the key is corrupted or inaccessible, EncryptedSharedPreferences might fail
            // Fallback to regular SharedPreferences is unsafe for secrets, so we let it throw
            // and handle at the call site or crash as it's a critical failure.
            throw e
        }
    }

    /**
     * Gets or generates a random 64-byte key for database encryption.
     * This key is stored in EncryptedSharedPreferences (which is backed by Keystore).
     */
    fun getDatabaseEncryptionKey(): ByteArray {
        val keyHex = securePrefs.getString(DB_ENCRYPTION_KEY, null)
        if (keyHex != null) {
            return hexToBytes(keyHex)
        }

        val key = ByteArray(64)
        secureRandom.nextBytes(key)
        val newKeyHex = bytesToHex(key)
        securePrefs.edit { putString(DB_ENCRYPTION_KEY, newKeyHex) }
        return key
    }

    /**
     * Utility to store a sensitive string (like an API key) securely.
     */
    fun storeSecret(
        key: String,
        value: String,
    ) {
        securePrefs.edit { putString(key, value) }
    }

    /**
     * Utility to retrieve a sensitive string securely.
     */
    fun getSecret(key: String): String? = securePrefs.getString(key, null)

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val bytes = ByteArray(hex.length / 2)
        for (i in 0 until hex.length step 2) {
            bytes[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
        }
        return bytes
    }
}
