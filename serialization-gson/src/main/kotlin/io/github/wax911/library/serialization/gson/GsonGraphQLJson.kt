package io.github.wax911.library.serialization.gson

import com.google.gson.Gson
import io.github.wax911.library.model.GraphQLJson
import java.lang.reflect.Type

/**
 * Gson-backed implementation of [GraphQLJson].
 *
 * @param gson A configured [Gson] instance. Defaults to a plain [Gson].
 */
class GsonGraphQLJson(
    private val gson: Gson = Gson(),
) : GraphQLJson {

    override fun <T : Any> encode(value: T, type: Type?): String =
        if (type != null) gson.toJson(value, type) else gson.toJson(value)

    override fun <T : Any> decode(json: String, type: Type): T =
        gson.fromJson(json, type)
}
