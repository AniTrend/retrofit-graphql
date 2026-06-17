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

import com.google.gson.Gson
import co.anitrend.retrofit.graphql.model.GraphQLJson
import java.lang.reflect.Type

/**
 * Gson-backed implementation of [GraphQLJson].
 *
 * @param gson A configured [Gson] instance. Defaults to a plain [Gson].
 */
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
