package io.github.wax911.library.model

import java.lang.reflect.Type

/**
 * Pluggable serialization abstraction for GraphQL request and response bodies.
 *
 * Implementations exist for Gson, kotlinx.serialization, and any other JSON
 * serialization framework that consumers prefer.
 */
interface GraphQLJson {

    /**
     * Encode [value] into a JSON string.
     *
     * @param value The value to serialize.
     * @param type  Optional [Type] token for generic types. When null, the
     *              serializer should use the runtime class of [value].
     * @return The JSON string representation.
     */
    fun <T : Any> encode(value: T, type: Type? = null): String

    /**
     * Decode a JSON string into an instance of [T].
     *
     * @param json The JSON string to deserialize.
     * @param type The target [Type] token (e.g. [GraphContainer] parameterized type).
     * @return The deserialized value.
     */
    fun <T : Any> decode(json: String, type: Type): T
}
