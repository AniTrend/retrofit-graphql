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

package co.anitrend.retrofit.graphql.serialization.gson

import co.anitrend.retrofit.graphql.model.GraphQLJson
import com.google.gson.Gson
import java.lang.reflect.Type

/**
 * Gson-backed implementation of [GraphQLJson].
 *
 * **Legacy**: lives in `:compat` for the deprecated
 * [co.anitrend.retrofit.graphql.converter.GraphConverter] flow. New code
 * should use [co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLTransportCodec]
 * from `:serialization-gson` with the backend-neutral
 * [co.anitrend.retrofit.graphql.converter.GraphQLConverterFactory].
 *
 * @param gson A configured [Gson] instance. Defaults to a plain [Gson].
 */
@Deprecated(
    "Legacy GraphQLJson implementation of the GraphConverter flow. " +
        "Use GsonGraphQLTransportCodec with the backend-neutral GraphQLConverterFactory instead.",
)
class GsonGraphQLJson(
    private val gson: Gson = Gson(),
) : GraphQLJson {
    override fun <T : Any> encode(
        value: T,
        type: Type?,
    ): String = if (type != null) gson.toJson(value, type) else gson.toJson(value)

    override fun <T : Any> decode(
        json: String,
        type: Type,
    ): T = gson.fromJson(json, type)
}
