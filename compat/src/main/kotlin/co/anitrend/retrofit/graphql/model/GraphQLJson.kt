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

package co.anitrend.retrofit.graphql.model

import java.lang.reflect.Type

/**
 * Pluggable serialization abstraction for GraphQL request and response bodies.
 *
 * This is the **legacy** serialization seam of the deprecated [GraphConverter]
 * path. It is retained in `:compat` for backward compatibility; new code
 * should use [co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec]
 * instead, which is backend-neutral and consumed by the new
 * [co.anitrend.retrofit.graphql.converter.GraphQLConverterFactory].
 *
 * ## Implementations
 *
 * | Implementation | Module | Notes |
 * |---------------|--------|-------|
 * | [GsonGraphQLJson] | `:compat` | Gson-backed. Handles all types including [GraphContainer.extensions] and [GraphQLRequest.extensions]. |
 * | [KotlinxGraphQLJson] | `:compat` | kotlinx.serialization-backed. Handles parameterized types via JVM reflection extension. [GraphContainer.extensions] and [GraphQLRequest.extensions] are `@Transient`. Recommended for response DTOs. |
 *
 * ## Implementing a custom backend
 *
 * ```kotlin
 * class MoshiGraphQLJson(private val moshi: Moshi) : GraphQLJson {
 *     override fun <T : Any> encode(value: T, type: Type?): String = ...
 *     override fun <T : Any> decode(json: String, type: Type): T = ...
 * }
 * ```
 *
 * Register with `GraphConverter.create(context, json = MoshiGraphQLJson(...))`.
 *
 * @see GsonGraphQLJson
 * @see KotlinxGraphQLJson
 * @see co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
 */
@Deprecated(
    "Legacy GraphQLJson serialization seam for GraphConverter. " +
        "Use GraphQLTransportCodec with the backend-neutral GraphQLConverterFactory instead.",
)
interface GraphQLJson {
    /**
     * Encode [value] into a JSON string.
     *
     * @param value The value to serialize.
     * @param type  Optional [Type] token for generic types. When null, the
     *              serializer should use the runtime class of [value].
     * @return The JSON string representation.
     */
    fun <T : Any> encode(
        value: T,
        type: Type? = null,
    ): String

    /**
     * Decode a JSON string into an instance of [T].
     *
     * @param json The JSON string to deserialize.
     * @param type The target [Type] token (e.g. [GraphContainer] parameterized type).
     * @return The deserialized value.
     */
    fun <T : Any> decode(
        json: String,
        type: Type,
    ): T
}
