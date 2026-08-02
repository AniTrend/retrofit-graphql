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

package co.anitrend.retrofit.graphql.converter.request

import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Converter
import java.lang.reflect.Type

/**
 * Backend-neutral GraphQL request body converter.
 *
 * [GraphQLRequestConverter] is the neutral counterpart of the legacy
 * [GraphRequestConverter]. It accepts only [GraphQLOperationRequest] bodies
 * and delegates serialization entirely to a [GraphQLTransportCodec]; it never
 * references a JSON backend, a document registry, or asset discovery.
 *
 * ## Type forwarding
 *
 * The complete Java [Type] of the body parameter as declared by the Retrofit
 * method (for example `GraphQLOperationRequest<GetUserVariables>`) is
 * forwarded to [GraphQLTransportCodec.encodeRequest] so the codec can resolve
 * the typed variables serializer without reflection on the runtime class.
 *
 * ## Failure behavior
 *
 * - Unsupported body types fail with [IllegalArgumentException] naming both
 *   the actual runtime type and the expected neutral type.
 * - Codec failures propagate as
 *   [co.anitrend.retrofit.graphql.serialization.GraphQLRequestEncodingException],
 *   which already carries the operation name and request type context.
 *
 * @param codec The explicit [GraphQLTransportCodec] used to serialize bodies.
 * @param type The complete Java reflection [Type] of the request body
 *   parameter, including generic arguments, as provided by Retrofit.
 *
 * @see GraphQLConverterFactory
 * @see GraphQLOperationRequest
 */
class GraphQLRequestConverter(
    private val codec: GraphQLTransportCodec,
    private val type: Type,
) : Converter<Any, RequestBody> {
    /**
     * Converts a [GraphQLOperationRequest] into a JSON request body.
     *
     * @param value The request body object. Must be a
     *   [GraphQLOperationRequest]; any other type fails with
     *   [IllegalArgumentException].
     * @return The serialized JSON [RequestBody].
     * @throws IllegalArgumentException when [value] is not a
     *   [GraphQLOperationRequest].
     * @throws co.anitrend.retrofit.graphql.serialization.GraphQLRequestEncodingException
     *   when the codec fails to serialize the request.
     */
    override fun convert(value: Any): RequestBody {
        val request =
            value as? GraphQLOperationRequest<*>
                ?: throw IllegalArgumentException(
                    "Unsupported request body type: ${value.javaClass.name}. " +
                        "Expected GraphQLOperationRequest.",
                )
        val body = codec.encodeRequest(request, type)
        return body.toRequestBody(MEDIA_TYPE)
    }

    companion object {
        private val MEDIA_TYPE = "application/json".toMediaTypeOrNull()
    }
}
