/**
 * Copyright 2021 AniTrend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.wax911.library.persistedquery

@Deprecated(
    "Consider migrating to AutomaticPersistedQueryErrors instead",
    ReplaceWith(
        "AutomaticPersistedQueryErrors",
        "io.github.wax911.library.persisted.query.error.AutomaticPersistedQueryErrors"
    )
)
object PersistedQueryErrors {
    // server does not support Automatic Persisted Queries. The client should fallback to regular query
    const val APQ_NOT_SUPPORTED_ERROR = "PersistedQueryNotSupported"
    // the server has yet seen the query content. Client should fallback to regular query
    const val APQ_QUERY_NOT_FOUND_ERROR = "PersistedQueryNotFound"
}
