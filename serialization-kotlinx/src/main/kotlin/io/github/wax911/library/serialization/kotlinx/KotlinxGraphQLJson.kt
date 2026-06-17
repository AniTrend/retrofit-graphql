package io.github.wax911.library.serialization.kotlinx

import io.github.wax911.library.model.GraphQLJson
import kotlinx.serialization.json.Json
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
    override fun <T : Any> encode(value: T, type: Type?): String {
        val serializer = json.serializersModule.serializer(
            (type as? Class<T>) ?: (value::class.java as Class<T>)
        )
        return json.encodeToString(
            serializer as kotlinx.serialization.SerializationStrategy<T>,
            value,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> decode(jsonStr: String, type: Type): T {
        val serializer = json.serializersModule.serializer(type as Class<T>)
        return json.decodeFromString(serializer, jsonStr)
    }
}
