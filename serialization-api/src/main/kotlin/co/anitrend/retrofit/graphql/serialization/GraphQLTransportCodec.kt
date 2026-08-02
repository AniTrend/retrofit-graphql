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

package co.anitrend.retrofit.graphql.serialization

import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import java.lang.reflect.Type

/**
 * Backend-neutral contract for serializing GraphQL requests and responses.
 *
 * [GraphQLTransportCodec] is the seam between the neutral protocol contracts
 * in `:api` and a concrete JSON backend (Gson, kotlinx.serialization, Moshi,
 * and so on). It deliberately references no serializer, Android, Retrofit, or
 * OkHttp types: implementations only see [GraphQLOperationRequest], raw
 * [String] bodies, and [Type] tokens.
 *
 * This is the contract future runtime code (APQ support, converter rewiring)
 * will invoke. Implementations should be stateless and thread-safe.
 *
 * ## Exception policy
 *
 * Implementations must never leak backend-specific exceptions. Any failure
 * while encoding or decoding must be wrapped in
 * [GraphQLRequestEncodingException] or [GraphQLResponseDecodingException]
 * respectively, so callers can attach operation and [Type] context to the
 * failure without depending on the backend.
 *
 * ## Usage
 *
 * ```kotlin
 * class MyCodec(private val json: MyJson) : GraphQLTransportCodec {
 *     override fun encodeRequest(
 *         request: GraphQLOperationRequest<*>,
 *         requestType: Type,
 *     ): String = try {
 *         json.encode(request, requestType)
 *     } catch (cause: Exception) {
 *         throw GraphQLRequestEncodingException(
 *             operationName = request.operationName,
 *             requestType = requestType,
 *             message = "Failed to encode GraphQL request",
 *             cause = cause,
 *         )
 *     }
 *
 *     override fun <T : Any> decodeResponse(
 *         body: String,
 *         responseType: Type,
 *     ): T = try {
 *         json.decode(body, responseType)
 *     } catch (cause: Exception) {
 *         throw GraphQLResponseDecodingException(
 *             responseType = responseType,
 *             message = "Failed to decode GraphQL response",
 *             cause = cause,
 *         )
 *     }
 * }
 * ```
 *
 * @see GraphQLRequestEncodingException
 * @see GraphQLResponseDecodingException
 */
interface GraphQLTransportCodec {
    /**
     * Serializes a GraphQL operation request into a raw body string.
     *
     * @param request The operation request to serialize.
     * @param requestType The complete [Type] of [request] as declared by the
     *   caller, including generic arguments (for example
     *   `GraphQLOperationRequest<GetUserVariables>`).
     * @return The serialized body string.
     * @throws GraphQLRequestEncodingException when serialization fails.
     */
    fun encodeRequest(
        request: GraphQLOperationRequest<*>,
        requestType: Type,
    ): String

    /**
     * Deserializes a raw GraphQL response body into [T].
     *
     * @param body The raw response body string.
     * @param responseType The complete [Type] of the expected response,
     *   including generic arguments (for example
     *   `GraphQLResponse<GetUserData>`).
     * @return The deserialized response.
     * @throws GraphQLResponseDecodingException when deserialization fails.
     */
    fun <T : Any> decodeResponse(
        body: String,
        responseType: Type,
    ): T
}
