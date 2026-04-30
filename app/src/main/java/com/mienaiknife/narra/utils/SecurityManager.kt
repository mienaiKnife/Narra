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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Gets or generates a random 64-byte key for database encryption.
     * This key is stored in EncryptedSharedPreferences (which is backed by Keystore).
     */
    fun getDatabaseEncryptionKey(): ByteArray {
        val keyHex = securePrefs.getString("db_encryption_key", null)
        if (keyHex != null) {
            return hexToBytes(keyHex)
        }

        val key = ByteArray(64)
        SecureRandom().nextBytes(key)
        val newKeyHex = bytesToHex(key)
        securePrefs.edit().putString("db_encryption_key", newKeyHex).apply()
        return key
    }

    /**
     * Utility to store a sensitive string (like an API key) securely.
     */
    fun storeSecret(key: String, value: String) {
        securePrefs.edit().putString(key, value).apply()
    }

    /**
     * Utility to retrieve a sensitive string securely.
     */
    fun getSecret(key: String): String? {
        return securePrefs.getString(key, null)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val bytes = ByteArray(hex.length / 2)
        for (i in 0 until hex.length step 2) {
            bytes[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
        }
        return bytes
    }
}
