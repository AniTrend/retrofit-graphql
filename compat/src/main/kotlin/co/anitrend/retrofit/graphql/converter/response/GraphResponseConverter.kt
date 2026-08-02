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

package co.anitrend.retrofit.graphql.converter.response

import co.anitrend.retrofit.graphql.model.GraphQLJson
import okhttp3.ResponseBody
import retrofit2.Converter
import java.io.IOException
import java.lang.reflect.Type

/**
 * GraphQL response body converter to unwrap nested object results,
 * resulting in a smaller generic tree for requests.
 *
 * **Legacy**: this converter lives in `:compat` for the deprecated
 * [co.anitrend.retrofit.graphql.converter.GraphConverter] flow. New code
 * should use the backend-neutral
 * [co.anitrend.retrofit.graphql.converter.response.GraphQLResponseConverter]
 * from `:runtime`.
 *
 * ## Serialization
 *
 * Response deserialization is delegated to [GraphQLJson.decode]. The full
 * parameterized [type] (e.g. `GraphContainer<GetCurrentUserData>`) is
 * forwarded so that serializers with parameterized type awareness (e.g.
 * [KotlinxGraphQLJson] via `kotlinx.serialization.serializer(type)`) can
 * resolve the correct `KSerializer`.
 *
 * [GraphQLJson.decode] may throw if the response JSON is invalid or if no
 * serializer can be resolved for the target type. These errors propagate
 * to Retrofit's caller.
 *
 * @param type The target deserialization type including parameterized type info
 *   (e.g. `GraphContainer<Foo>`). Must not be null when called from Retrofit.
 * @param json Pluggable [GraphQLJson] instance for deserialization.
 *
 * @see co.anitrend.retrofit.graphql.converter.response.GraphQLResponseConverter
 */
@Deprecated(
    "Legacy response converter of the GraphConverter flow. Use GraphQLResponseConverter with an explicit GraphQLTransportCodec instead.",
)
open class GraphResponseConverter<T>(
    protected val type: Type?,
    protected val json: GraphQLJson,
) : Converter<ResponseBody, T> {
    /**
     * Converter contains logic on how to handle responses, since GraphQL responses follow
     * the JsonAPI spec it makes sense to wrap our base query response data and errors response
     * in here, the logic remains open to the implementation
     * <br></br>
     *
     * @param responseBody The retrofit response body received from the network
     * @return The type declared in the Call of the request
     */
    override fun convert(responseBody: ResponseBody): T? {
        var response: T? = null
        try {
            val responseString = responseBody.string()
            val resolvedType =
                type ?: throw IllegalStateException(
                    "Response type cannot be null when converting response body. " +
                        "This indicates a Retrofit configuration issue.",
                )
            response = json.decode(responseString, resolvedType)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return response
    }
}
