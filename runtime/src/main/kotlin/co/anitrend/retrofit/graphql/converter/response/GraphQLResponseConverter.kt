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

package co.anitrend.retrofit.graphql.converter.response

import co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
import okhttp3.ResponseBody
import retrofit2.Converter
import java.io.IOException
import java.lang.reflect.Type

/**
 * Backend-neutral GraphQL response body converter.
 *
 * [GraphQLResponseConverter] is the neutral counterpart of the legacy
 * [GraphResponseConverter]. It delegates deserialization entirely to a
 * [GraphQLTransportCodec] and never references a JSON backend.
 *
 * ## Type and body forwarding
 *
 * The raw response body string and the complete Java [Type] of the response
 * as declared by the Retrofit method (for example
 * `GraphQLResponse<GetCurrentUserData>`) are forwarded to
 * [GraphQLTransportCodec.decodeResponse] so the codec can resolve the
 * parameterized data serializer.
 *
 * ## Failure behavior
 *
 * - Codec failures propagate as
 *   [co.anitrend.retrofit.graphql.serialization.GraphQLResponseDecodingException],
 *   which already carries the response type context.
 * - I/O failures while reading the response body propagate as [IOException]
 *   with the target response type attached to the message.
 * - Failures are never printed and never converted to null: conversion either
 *   returns a decoded value or throws.
 *
 * @param T The decoded response type, typically
 *   [co.anitrend.retrofit.graphql.model.GraphQLResponse] of a data type.
 * @param type The complete Java reflection [Type] of the response, including
 *   generic arguments, as provided by Retrofit.
 * @param codec The explicit [GraphQLTransportCodec] used to deserialize
 *   bodies.
 *
 * @see GraphQLConverterFactory
 * @see GraphQLTransportCodec
 */
class GraphQLResponseConverter<T : Any>(
    private val type: Type,
    private val codec: GraphQLTransportCodec,
) : Converter<ResponseBody, T> {
    /**
     * Converts a raw [ResponseBody] into a decoded GraphQL response.
     *
     * @param responseBody The Retrofit response body received from the network.
     * @return The decoded value of type [T].
     * @throws IOException when the response body cannot be read.
     * @throws co.anitrend.retrofit.graphql.serialization.GraphQLResponseDecodingException
     *   when the codec fails to deserialize the response.
     */
    override fun convert(responseBody: ResponseBody): T {
        val body =
            try {
                responseBody.string()
            } catch (cause: IOException) {
                throw IOException(
                    "Failed to read the GraphQL response body for response type $type",
                    cause,
                )
            }
        return codec.decodeResponse(body, type)
    }
}
