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

package co.anitrend.retrofit.graphql.model.attribute

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * GraphQL error representation that is spec compliant.
 *
 * Annotated with [kotlinx.serialization.Serializable] to enable
 * deserialization as part of [GraphContainer.errors] when using kotlinx.
 *
 * ## Serialization Behavior
 *
 * ### kotlinx.serialization
 * - [message] and [locations] are deserialized normally
 * - [path] is `@Transient` (excluded) because `List<Any>` cannot be
 *   statically resolved by the kotlinx compiler plugin
 * - [extensions] is `@Transient` (excluded) because `Map<String, Any?>`
 *   cannot be statically resolved by the kotlinx compiler plugin
 *
 * ### Gson (via GsonGraphQLJson)
 * - All fields are serialized/deserialized normally, including [path]
 *   and [extensions]
 *
 * ### Accessing [path] and [extensions] with kotlinx
 *
 * To read [path] or [extensions] while using kotlinx serialization, define
 * your own `@Serializable` error class with a custom serializer:
 * ```kotlin
 * @Serializable(with = GraphErrorPathExtensionsSerializer::class)
 * data class MyGraphError(
 *     val message: String? = null,
 *     val path: List<JsonElement>? = null,
 *     val locations: List<Location>? = null,
 *     val extensions: Map<String, JsonElement>? = null,
 * )
 *
 * object GraphErrorPathExtensionsSerializer :
 *     JsonTransformingSerializer<MyGraphError>(MyGraphError.serializer()) {
 *     override fun transformDeserialize(element: JsonElement): JsonElement {
 *         // path and extensions are available in the raw JSON object
 *         return element
 *     }
 * }
 * ```
 * Alternatively, extract [path] and [extensions] from the raw [JsonObject]
 * payload before kotlinx deserialization runs.
 *
 * @param message Description of the error.
 * @param path Path of the response field that encountered the error.
 *   Gson-only; `@Transient` for kotlinx.
 * @param locations List of locations within the GraphQL document at which the error occurred.
 * @param extensions Additional information about the error.
 *   Gson-only; `@Transient` for kotlinx.
 *
 * @see [GraphQL Error Specification](http://spec.graphql.org/June2018/#sec-Errors)
 * @see GraphContainer
 */
@Serializable
data class GraphError(
    val message: String? = null,
    @Transient
    val path: List<Any>? = null,
    val locations: List<Location>? = null,
    @Transient
    val extensions: Map<String, Any?>? = null,
) {
    /**
     * Location describing which part of GraphQL document caused an exception.
     */
    @Serializable
    data class Location(
        val line: Int = 0,
        val column: Int = 0,
    )

    override fun toString(): String {
        return "GraphError{" +
            "message='" + message + '\''.toString() +
            ", path=" + path?.joinToString() +
            ", locations=" + locations?.joinToString() +
            ", extensions=" + extensions +
            '}'.toString()
    }
}
