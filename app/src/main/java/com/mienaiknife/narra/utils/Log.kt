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

/**
 * A wrapper for android.util.Log that can be safely used in unit tests
 * where android.util.Log is not available/mocked.
 */
object Log {
    private var isTesting = false
    
    init {
        try {
            // Check if we are running in a unit test environment (no Android framework)
            // or if android.util.Log is just a stub that throws exceptions.
            val logClass = Class.forName("android.util.Log")
            // Try to access a method that might be missing or native
            logClass.getMethod("d", String::class.java, String::class.java)
            
            // If we are in a Robolectric or real Android environment, this might work.
            // But in a pure JUnit test with the "mockable android jar", 
            // the methods exist but are either empty or throw exceptions if not configured.
            // The UnsatisfiedLinkError suggests a native method call failed.
        } catch (e: Exception) {
            isTesting = true
        }
    }

    fun d(tag: String, msg: String) {
        if (isTesting) {
            println("D/$tag: $msg")
        } else {
            try {
                android.util.Log.d(tag, msg)
            } catch (e: Throwable) {
                isTesting = true
                println("D/$tag: $msg")
            }
        }
    }

    fun i(tag: String, msg: String) {
        if (isTesting) {
            println("I/$tag: $msg")
        } else {
            try {
                android.util.Log.i(tag, msg)
            } catch (e: Throwable) {
                isTesting = true
                println("I/$tag: $msg")
            }
        }
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (isTesting) {
            println("E/$tag: $msg")
            tr?.printStackTrace()
        } else {
            try {
                android.util.Log.e(tag, msg, tr)
            } catch (e: Throwable) {
                isTesting = true
                println("E/$tag: $msg")
                tr?.printStackTrace()
            }
        }
    }

    fun w(tag: String, msg: String) {
        if (isTesting) {
            println("W/$tag: $msg")
        } else {
            try {
                android.util.Log.w(tag, msg)
            } catch (e: Throwable) {
                isTesting = true
                println("W/$tag: $msg")
            }
        }
    }
}
