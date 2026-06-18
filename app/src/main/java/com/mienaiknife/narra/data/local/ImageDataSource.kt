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

/**
 * Interface for downloading and persisting images to local storage.
 */
interface ImageDataSource {
    /**
     * Downloads an image from the given URL and saves it to a persistent local file.
     * Returns the absolute path to the saved image file, or null if the download fails.
     */
    suspend fun downloadAndSaveImage(
        url: String,
        fileName: String,
    ): String?

    /**
     * Saves a byte array as an image to a persistent local file.
     * Returns the absolute path to the saved image file.
     */
    suspend fun saveImage(
        data: ByteArray,
        fileName: String,
    ): String?
}
