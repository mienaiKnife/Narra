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

package com.mienaiknife.narra.domain

sealed class NarraError : Throwable() {
    sealed class Network : NarraError() {
        object NoConnection : Network()
        object Timeout : Network()
        data class ServerError(val code: Int, override val message: String?) : Network()
        object WifiRequired : Network()
    }

    sealed class Content : NarraError() {
        object EmptyContent : Content()
        object ParsingFailed : Content()
        object ArticleAlreadyInQueue : Content()
        object NotFound : Content()
        object InvalidFeed : Content()
    }

    sealed class Storage : NarraError() {
        object DiskFull : Storage()
        data class AccessDenied(override val message: String?) : Storage()
    }

    data class Unknown(override val cause: Throwable?) : NarraError()
}
