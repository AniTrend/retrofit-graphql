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

package io.github.wax911.library.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import io.github.wax911.library.model.attribute.GraphError
import io.github.wax911.library.model.body.GraphContainer
import retrofit2.Response

/**
 * Converts the response error response into an object.
 *
 * @return The error object, or null if an exception was encountered
 *
 * @see GraphError
 */
fun Response<*>?.getError(): List<GraphError>? {
    try {
        if (this != null) {
            val responseBody = errorBody()
            val message = responseBody?.string()
            if (responseBody != null && !message.isNullOrBlank()) {
                val graphErrors = message.getGraphQLError()
                if (graphErrors != null) {
                    return graphErrors
                }
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return null
}

@Throws(JsonSyntaxException::class)
private fun String.getGraphQLError(): List<GraphError>? {
    val tokenType = object : TypeToken<GraphContainer<*>>() {}.type
    val graphContainer = Gson().fromJson<GraphContainer<*>>(this, tokenType)
    return graphContainer.errors
}
