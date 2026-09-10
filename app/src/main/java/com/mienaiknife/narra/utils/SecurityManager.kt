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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "SecurityManager"
        private const val PREFS_NAME = "secure_settings"
        private const val DB_ENCRYPTION_KEY = "db_encryption_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "narra_secrets_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256
        private const val DATABASE_KEY_SIZE_BYTES = 64
    }

    private val secureRandom = SecureRandom()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Gets or generates a random 64-byte key for database encryption. The key is
     * wrapped with an Android Keystore AES key before being persisted. If the stored
     * value cannot be unwrapped (e.g. the Keystore key was lost), a new key is
     * generated rather than crashing; the database layer then treats the existing
     * database as unreadable and recovers by starting fresh.
     */
    fun getDatabaseEncryptionKey(): ByteArray {
        val stored = prefs.getString(DB_ENCRYPTION_KEY, null)
        if (stored != null) {
            val key = runCatching { decrypt(stored) }.getOrNull()
            if (key != null && key.size == DATABASE_KEY_SIZE_BYTES) {
                return key
            }
            Log.w(TAG, "Stored database key is missing or corrupt; generating a new one")
        }

        val key = ByteArray(DATABASE_KEY_SIZE_BYTES)
        secureRandom.nextBytes(key)
        storeDatabaseKey(key)
        return key
    }

    private fun storeDatabaseKey(key: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        val blob = cipher.iv + cipher.doFinal(key)
        prefs.edit { putString(DB_ENCRYPTION_KEY, Base64.encodeToString(blob, Base64.NO_WRAP)) }
    }

    private fun decrypt(encoded: String): ByteArray {
        val blob = Base64.decode(encoded, Base64.NO_WRAP)
        require(blob.size > GCM_IV_LENGTH_BYTES) { "Encrypted blob is too short" }
        val iv = blob.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = blob.copyOfRange(GCM_IV_LENGTH_BYTES, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return keyGenerator.generateKey()
    }
}
