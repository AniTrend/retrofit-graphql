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

import java.math.BigDecimal

/**
 * Backend-neutral representation of an arbitrary GraphQL JSON value.
 *
 * [GraphQLValue] models the six JSON value shapes that can appear anywhere
 * in a GraphQL payload (request extensions, response extensions, error
 * extensions, error paths, and arbitrary vendor fields). It deliberately
 * carries no serializer, platform, or HTTP framework types so that any
 * JSON backend (Gson, kotlinx.serialization, Moshi, and so on) can map its
 * own element model onto this hierarchy.
 *
 * The hierarchy is exhaustive:
 * - [Null] for explicit JSON `null`
 * - [BooleanValue] for JSON booleans
 * - [StringValue] for JSON strings
 * - [NumberValue] for JSON numbers
 * - [ListValue] for JSON arrays
 * - [ObjectValue] for JSON objects with String keys
 *
 * ## Numeric precision
 *
 * Numbers are held as [BigDecimal] so that arbitrary-precision JSON numbers
 * survive a round trip without floating point loss. Note that
 * `BigDecimal.equals` is scale-sensitive (`1.0` and `1.00` are not equal);
 * use `compareTo` when only the numeric value matters.
 *
 * ## Usage
 *
 * ```kotlin
 * val extension = GraphQLValue.ObjectValue(
 *     fields = mapOf(
 *         "persistedQuery" to GraphQLValue.ObjectValue(
 *             fields = mapOf(
 *                 "version" to GraphQLValue.NumberValue(BigDecimal(1)),
 *                 "sha256Hash" to GraphQLValue.StringValue("abc123"),
 *             ),
 *         ),
 *     ),
 * )
 * ```
 *
 * @see GraphQLResponse
 * @see GraphQLResponseError
 * @see GraphQLOperationRequest
 */
sealed interface GraphQLValue {
    /**
     * Explicit JSON `null`.
     *
     * Distinct from a missing field: a field mapped to [Null] is
     * present on the wire with the value `null`.
     */
    data object Null : GraphQLValue

    /**
     * JSON boolean value.
     *
     * @property value The boolean payload.
     */
    data class BooleanValue(
        val value: Boolean,
    ) : GraphQLValue

    /**
     * JSON string value.
     *
     * @property value The string payload.
     */
    data class StringValue(
        val value: String,
    ) : GraphQLValue

    /**
     * JSON number value with full decimal precision.
     *
     * @property value The number payload, preserved exactly as [BigDecimal].
     */
    data class NumberValue(
        val value: BigDecimal,
    ) : GraphQLValue

    /**
     * JSON array value.
     *
     * @property values The element values, each recursively a [GraphQLValue].
     */
    data class ListValue(
        val values: List<GraphQLValue>,
    ) : GraphQLValue

    /**
     * JSON object value with String keys.
     *
     * Use this type for arbitrary JSON fields that do not have a dedicated
     * contract, such as the contents of [GraphQLResponse.extensions],
     * [GraphQLResponseError.extensions], or any vendor-specific payload.
     *
     * @property fields The key/value pairs of the object.
     */
    data class ObjectValue(
        val fields: Map<String, GraphQLValue>,
    ) : GraphQLValue
}
