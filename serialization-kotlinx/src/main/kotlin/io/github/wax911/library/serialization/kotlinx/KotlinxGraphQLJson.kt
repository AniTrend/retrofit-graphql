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

package io.github.wax911.library.serialization.kotlinx

import io.github.wax911.library.model.GraphQLJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.reflect.Type

/**
 * kotlinx.serialization-backed implementation of [GraphQLJson].
 *
 * @param json A configured [Json] instance. Defaults to [Json] with [Json.ignoreUnknownKeys].
 */
class KotlinxGraphQLJson(
    private val json: Json = Json { ignoreUnknownKeys = true },
) : GraphQLJson {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> encode(
        value: T,
        type: Type?,
    ): String {
        val clazz = (type as? Class<T>) ?: (value::class.java as Class<T>)
        val serializer = json.serializersModule.serializer(clazz as Type) as kotlinx.serialization.KSerializer<T>
        return json.encodeToString(serializer, value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> decode(
        jsonStr: String,
        type: Type,
    ): T {
        val clazz = type as Class<T>
        val serializer = json.serializersModule.serializer(clazz as Type) as kotlinx.serialization.KSerializer<T>
        return json.decodeFromString(serializer, jsonStr)
    }
}
