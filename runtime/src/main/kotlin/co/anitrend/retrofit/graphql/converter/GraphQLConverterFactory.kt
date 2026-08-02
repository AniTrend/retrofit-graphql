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

package co.anitrend.retrofit.graphql.converter

import co.anitrend.retrofit.graphql.converter.request.GraphQLRequestConverter
import co.anitrend.retrofit.graphql.converter.response.GraphQLResponseConverter
import co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Backend-neutral [Converter.Factory] for Retrofit GraphQL endpoints.
 *
 * [GraphQLConverterFactory] is the neutral counterpart of the legacy
 * [GraphConverter]. It is the only factory in `:runtime` whose request and
 * response conversion is fully delegated to a [GraphQLTransportCodec]; it
 * never references a JSON backend, an Android [android.content.Context], or
 * asset discovery.
 *
 * ## Explicit codec ownership
 *
 * Every creation path requires an explicit [GraphQLTransportCodec]. There is
 * no default codec, no reflection-based codec selection, no backend
 * instantiation, and no retry/fallback: the codec instance passed to
 * [create] (or to the constructor) is the only serialization path used.
 *
 * ```kotlin
 * val factory = GraphQLConverterFactory.create(
 *     codec = GsonGraphQLTransportCodec(),
 * )
 * Retrofit.Builder()
 *     .baseUrl("https://api.github.com/graphql")
 *     .addConverterFactory(factory)
 *     .build()
 * ```
 *
 * ## Request contract
 *
 * Request conversion accepts only [GraphQLOperationRequest] bodies. Any other
 * body type fails during conversion with an [IllegalArgumentException] that
 * names both the actual type and the expected neutral type.
 *
 * ## Registry semantics
 *
 * [create] optionally accepts a [GraphQLDocumentRegistry] for API parity with
 * legacy factories and for the upcoming persisted query (APQ) phase. Because
 * [GraphQLOperationRequest] always carries its document, request conversion
 * never consults the registry: no hidden lookup is performed while the
 * neutral request already contains its query.
 *
 * ## Capability boundaries
 *
 * - The factory owns every response type except [ResponseBody] itself, so it
 *   is intended for GraphQL-only endpoints.
 * - Codec failures propagate as [co.anitrend.retrofit.graphql.serialization.GraphQLRequestEncodingException]
 *   or [co.anitrend.retrofit.graphql.serialization.GraphQLResponseDecodingException];
 *   response conversion never prints failures or converts them to null.
 * - The legacy [GraphConverter] path is isolated in `:compat`; `:runtime`
 *   carries no Gson or kotlinx.serialization dependency.
 *
 * @param codec The explicit [GraphQLTransportCodec] used for every request
 *   and response conversion performed by this factory.
 * @param registry Optional [GraphQLDocumentRegistry], retained for API parity
 *   and the upcoming APQ phase. Never consulted while the neutral request
 *   carries its document.
 *
 * @see GraphQLRequestConverter
 * @see GraphQLResponseConverter
 * @see GraphQLTransportCodec
 * @see GraphConverter
 */
class GraphQLConverterFactory(
    private val codec: GraphQLTransportCodec,
    private val registry: GraphQLDocumentRegistry? = null,
) : Converter.Factory() {
    /**
     * Returns the [GraphQLResponseConverter] used to decode response bodies
     * through the factory's [GraphQLTransportCodec].
     *
     * [ResponseBody] return types yield `null` from this factory so the
     * built-in Retrofit converter passes the raw body through, letting
     * callers inspect raw responses without decoding.
     *
     * @param type The response type declared on the Retrofit method.
     * @param annotations All annotations applied to the requesting method.
     * @param retrofit The [Retrofit] instance the converter is registered on.
     */
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        return if (type == ResponseBody::class.java) {
            null
        } else {
            GraphQLResponseConverter<Any>(type, codec)
        }
    }

    /**
     * Returns the [GraphQLRequestConverter] used to encode request bodies
     * through the factory's [GraphQLTransportCodec].
     *
     * The complete Java [type] of the body parameter (for example
     * `GraphQLOperationRequest<GetUserVariables>`) is forwarded so the codec
     * can resolve the typed variables serializer.
     *
     * @param type The type of the body parameter of the request.
     * @param parameterAnnotations All annotations applied to the parameter.
     * @param methodAnnotations All annotations applied to the method.
     * @param retrofit The [Retrofit] instance the converter is registered on.
     */
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? {
        return GraphQLRequestConverter(codec, type)
    }

    companion object {
        /**
         * Creates a [GraphQLConverterFactory] with an explicit
         * [GraphQLTransportCodec] and an optional [GraphQLDocumentRegistry].
         *
         * No [android.content.Context], default codec, or backend discovery is
         * involved: the codec is mandatory on every creation path.
         *
         * ```kotlin
         * val factory = GraphQLConverterFactory.create(
         *     registry = GeneratedGraphQLRegistry,
         *     codec = KotlinxGraphQLTransportCodec(),
         * )
         * ```
         *
         * @param codec The explicit [GraphQLTransportCodec] used for all
         *   request and response conversion.
         * @param registry Optional [GraphQLDocumentRegistry], retained for API
         *   parity and the upcoming APQ phase. Never consulted while the
         *   neutral request carries its document.
         * @return A new [GraphQLConverterFactory] instance.
         */
        @JvmOverloads
        fun create(
            codec: GraphQLTransportCodec,
            registry: GraphQLDocumentRegistry? = null,
        ): GraphQLConverterFactory = GraphQLConverterFactory(codec = codec, registry = registry)
    }
}
