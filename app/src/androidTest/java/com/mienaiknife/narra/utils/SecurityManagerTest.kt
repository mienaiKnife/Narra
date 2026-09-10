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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class SecurityManagerTest {

    private lateinit var context: Context
    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context
            .getSharedPreferences("secure_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry("narra_secrets_key")
        securityManager = SecurityManager(context)
    }

    @Test
    fun getDatabaseEncryptionKey_returnsStable64ByteKey() {
        val first = securityManager.getDatabaseEncryptionKey()
        val second = securityManager.getDatabaseEncryptionKey()

        assertEquals(64, first.size)
        assertArrayEquals("Key must be stable across calls", first, second)
    }

    @Test
    fun getDatabaseEncryptionKey_doesNotPersistKeyInPlaintext() {
        val key = securityManager.getDatabaseEncryptionKey()

        val stored =
            context
                .getSharedPreferences("secure_settings", Context.MODE_PRIVATE)
                .getString("db_encryption_key", null)

        assertNotNull("Encrypted key material should be persisted", stored)
        val plaintextHex = key.joinToString("") { "%02x".format(it) }
        assertFalse("Database key must not be stored in plaintext", stored!!.contains(plaintextHex))
    }
}
