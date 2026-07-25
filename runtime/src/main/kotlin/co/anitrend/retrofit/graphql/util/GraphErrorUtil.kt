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

package co.anitrend.retrofit.graphql.util

import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.model.attribute.GraphError
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import retrofit2.Response

/**
 * Converts the response error response into an object.
 *
 * Uses a default Gson-backed [GraphQLJson] instance for deserialization.
 * For custom serialization backends, use [getError] with a [GraphQLJson] parameter.
 *
 * @return The error object, or null if an exception was encountered
 *
 * @see GraphError
 */
fun Response<*>?.getError(): List<GraphError>? = getError(defaultGraphQLJson)

/**
 * Converts the response error response into an object using the provided [GraphQLJson].
 *
 * @param json A [GraphQLJson] instance used for deserialization.
 * @return The error object, or null if an exception was encountered
 *
 * @see GraphError
 */
fun Response<*>?.getError(json: GraphQLJson): List<GraphError>? {
    try {
        if (this != null) {
            val responseBody = errorBody()
            val message = responseBody?.string()
            if (responseBody != null && !message.isNullOrBlank()) {
                val graphErrors = message.getGraphQLError(json)
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

private fun String.getGraphQLError(json: GraphQLJson): List<GraphError>? {
    val tokenType = object : TypeToken<GraphContainer<*>>() {}.type
    val graphContainer = json.decode<GraphContainer<*>>(this, tokenType)
    return graphContainer.errors
}

/**
 * Default [GraphQLJson] instance used by the parameterless [getError] overload.
 * Uses the same Gson configuration as [co.anitrend.retrofit.graphql.converter.GraphConverter]'s default.
 */
private val defaultGraphQLJson: GraphQLJson by lazy {
    GsonGraphQLJson(
        GsonBuilder()
            .enableComplexMapKeySerialization()
            .serializeNulls()
            .setLenient()
            .create(),
    )
}
