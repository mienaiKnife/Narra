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

package com.mienaiknife.narra.domain.repository

import java.io.InputStream
import java.io.OutputStream

interface ImportExportRepository {
    suspend fun importEpub(inputStream: InputStream, title: String): Result<Unit>
    suspend fun importOpml(inputStream: InputStream): Result<Int>
    suspend fun exportOpml(outputStream: OutputStream): Result<Unit>
    suspend fun backupDatabase(outputStream: OutputStream): Result<Unit>
    suspend fun restoreDatabase(inputStream: InputStream): Result<Unit>
    suspend fun deleteAllMetadata()
}
