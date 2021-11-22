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

package io.github.wax911.library.model.request

import com.google.gson.Gson

/**
 * Query & Variable builder for URL parameter based GET requests
 */
class PersistedQueryUrlParameterBuilder(
    private val queryContainer: QueryContainer = QueryContainer(),
    private val gson: Gson
) {

    fun build(): PersistedQueryUrlParameters {
        return PersistedQueryUrlParameters(
            extensions = gson.toJson(queryContainer.extensions),
            operationName = queryContainer.operationName.orEmpty(),
            variables = gson.toJson(queryContainer.variables)
        )
    }

    companion object {
        const val EXTENSION_KEY_APQ = "persistedQuery"
    }
}
