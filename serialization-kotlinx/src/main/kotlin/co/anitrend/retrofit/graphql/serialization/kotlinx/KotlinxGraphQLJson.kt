/**
 * Copyright 2026 AniTrend
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

package co.anitrend.retrofit.graphql.serialization.kotlinx

import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.model.GraphQLRequest
import co.anitrend.retrofit.graphql.model.request.PersistedQuery
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import java.lang.reflect.Type

/**
 * kotlinx.serialization-backed implementation of [GraphQLJson].
 *
 * ## Parameterized Type Handling
 *
 * Uses [kotlinx.serialization.serializer] with `java.lang.reflect.Type` overload
 * to handle parameterized types (e.g. `GraphContainer<GetCurrentUserData>`,
 * `List<Bar>`) provided by Retrofit's converter infrastructure. This is the
 * key difference from a naive `serializer<T>()` call: Retrofit passes a
 * [java.lang.reflect.ParameterizedType], and the JVM reflection extension
 * `serializer(type)` resolves the correct [KSerializer] for the full type.
 *
 * ## Usage
 *
 * ```kotlin
 * val json = KotlinxGraphQLJson(
 *     Json {
 *         ignoreUnknownKeys = true
 *         encodeDefaults = false
 *     }
 * )
 * val converter = GraphConverter.create(
 *     context = context,
 *     json = json,
 *     registry = GeneratedGraphQLRegistry,
 * )
 * ```
 *
 * ## Limitations
 *
 * - [GraphContainer.extensions] remains `@Transient` because its
 *   `Map<Any, Any>` type cannot be statically resolved.
 * - [GraphQLRequest.extensions] remains `@Transient` on the data class, but
 *   [encode] merges supported extension values into the encoded JSON at runtime
 *   so `withPersistedQuery()` still works for the typed request flow.
 * - [GraphError.path] and [GraphError.extensions] are `@Transient` for the
 *   same reason.
 * - [QueryContainer] is not `@Serializable`; the runtime keeps the legacy
 *   [QueryContainerBuilder] request flow on a Gson-backed serializer.
 *
 * ## Working Around @Transient Fields
 *
 * When you need access to `@Transient` fields with kotlinx, define complete
 * transport wrapper classes that include those fields with concrete types that
 * kotlinx can resolve (e.g. `JsonObject` instead of `Map<Any, Any>`):
 * ```kotlin
 * @Serializable
 * data class JsonGraphContainer<T>(
 *     val data: T? = null,
 *     val errors: List<JsonGraphError>? = null,
 *     val extensions: JsonObject? = null,
 * )
 *
 * @Serializable
 * data class JsonGraphError(
 *     val message: String? = null,
 *     val path: List<JsonElement>? = null,
 *     val locations: List<GraphError.Location>? = null,
 *     val extensions: JsonObject? = null,
 * )
 * ```
 * Then use the wrapper as the Retrofit response type:
 * ```kotlin
 * @POST("graphql")
 * suspend fun getCurrentUser(
 *     @Body request: GraphQLRequest<EmptyGraphQLVariables>,
 * ): Response<JsonGraphContainer<GetCurrentUserData>>
 * ```
 * Use a custom serializer only when the wire JSON shape itself needs to be
 * transformed before or after normal serialization.
 * For detailed per-type examples, see the KDoc on [GraphContainer.extensions],
 * [GraphError.path], [GraphError.extensions], and [GraphQLRequest.extensions].
 *
 * @param json A configured [Json] instance. Defaults to [Json] with
 *   [Json.ignoreUnknownKeys] enabled so that fields not present in the
 *   generated data class are silently skipped.
 * @see GraphQLJson
 * @see GsonGraphQLJson
 */
class KotlinxGraphQLJson(
    private val json: Json = Json { ignoreUnknownKeys = true },
) : GraphQLJson {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> encode(
        value: T,
        type: Type?,
    ): String {
        if (value is GraphQLRequest<*>) {
            return encodeGraphQLRequest(value, type)
        }
        val resolvedType = type ?: value::class.java
        val serializer = resolveSerializer<T>(resolvedType)
        return json.encodeToString(serializer, value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> decode(
        jsonStr: String,
        type: Type,
    ): T {
        val serializer = resolveSerializer<T>(type)
        return json.decodeFromString(serializer, jsonStr)
    }

    /**
     * Resolves a [KSerializer] for the given [Type], handling [Class],
     * parameterized types, generic array types, and wildcard types.
     *
     * Uses the JVM extension [kotlinx.serialization.serializer] which supports
     * Java reflection types, including parameterized types like `GraphContainer<Foo>`.
     *
     * @param type The Java reflection [Type] to resolve a serializer for.
     * @return A [KSerializer] capable of serializing/deserializing values of [type].
     * @throws SerializationException if no serializer can be resolved for [type].
     * @throws IllegalArgumentException if [type] is an unsupported [Type] subclass.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> resolveSerializer(type: Type): KSerializer<T> {
        return try {
            json.serializersModule.serializer(type) as KSerializer<T>
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Unsupported type shape for kotlinx.serialization: $type (${type.javaClass.simpleName}). " +
                    "Supported types are Class, ParameterizedType, GenericArrayType, and WildcardType. " +
                    "Ensure the target class is annotated with @Serializable and the kotlinx.serialization " +
                    "compiler plugin is applied to its module.",
                e,
            )
        } catch (e: SerializationException) {
            throw SerializationException(
                "Cannot resolve kotlinx.serialization serializer for: $type. " +
                    "Ensure the target class and all its type arguments are annotated with @Serializable " +
                    "or registered as contextual serializers.",
                e,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun encodeGraphQLRequest(
        request: GraphQLRequest<*>,
        type: Type?,
    ): String {
        val resolvedType = type ?: request::class.java
        val serializer = resolveSerializer<GraphQLRequest<*>>(resolvedType)
        val baseJson = json.encodeToJsonElement(serializer, request).jsonObject.toMutableMap()

        if (request.extensions.isNotEmpty()) {
            baseJson["extensions"] = encodeDynamicMap(request.extensions)
        }

        return JsonObject(baseJson).toString()
    }

    private fun encodeDynamicMap(map: Map<String, Any?>): JsonObject =
        buildJsonObject {
            map.forEach { (key, value) ->
                put(key, encodeDynamicValue(value))
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun encodeDynamicValue(value: Any?): JsonElement =
        when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is PersistedQuery -> json.encodeToJsonElement(PersistedQuery.serializer(), value)
            is Map<*, *> ->
                buildJsonObject {
                    value.forEach { (entryKey, entryValue) ->
                        require(entryKey is String) {
                            "GraphQLRequest.extensions only supports String keys, but found ${entryKey?.javaClass?.name}"
                        }
                        put(entryKey, encodeDynamicValue(entryValue))
                    }
                }
            is Iterable<*> -> JsonArray(value.map(::encodeDynamicValue))
            is Array<*> -> JsonArray(value.map(::encodeDynamicValue))
            is Enum<*> -> encodeDynamicEnum(value)
            else -> {
                val serializer = resolveSerializer<Any>(value::class.java)
                json.encodeToJsonElement(serializer, value)
            }
        }

    /**
     * Encodes extension enum values with their generated kotlinx serializer when
     * one is available so enum-entry `@SerialName` values remain the wire name.
     * Non-serializable enum values fall back to [Enum.name] for backward
     * compatibility with ad-hoc extension maps.
     */
    private fun encodeDynamicEnum(value: Enum<*>): JsonElement =
        try {
            val serializer = resolveSerializer<Any>(value::class.java)
            json.encodeToJsonElement(serializer, value)
        } catch (_: SerializationException) {
            JsonPrimitive(value.name)
        } catch (_: IllegalArgumentException) {
            JsonPrimitive(value.name)
        }
}
