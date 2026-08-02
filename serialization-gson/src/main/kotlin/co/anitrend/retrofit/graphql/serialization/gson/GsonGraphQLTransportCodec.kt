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

import co.anitrend.retrofit.graphql.model.GraphQLData
import co.anitrend.retrofit.graphql.model.GraphQLPathSegment
import co.anitrend.retrofit.graphql.model.GraphQLResponse
import co.anitrend.retrofit.graphql.model.GraphQLResponseError
import co.anitrend.retrofit.graphql.model.GraphQLValue
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import co.anitrend.retrofit.graphql.serialization.GraphQLRequestEncodingException
import co.anitrend.retrofit.graphql.serialization.GraphQLResponseDecodingException
import co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Gson-backed implementation of [GraphQLTransportCodec].
 *
 * [GsonGraphQLTransportCodec] serializes [GraphQLOperationRequest] envelopes
 * and deserializes [GraphQLResponse] envelopes with Gson only inside this
 * module. It is the neutral counterpart of the legacy
 * [co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson] (which
 * moved to `:compat` with the deprecated `GraphConverter` flow) and never
 * routes through the generic
 * [co.anitrend.retrofit.graphql.model.GraphQLJson] seam.
 *
 * ## Request encoding
 *
 * [encodeRequest] writes the neutral envelope (`query`, `operationName`,
 * `variables`, `extensions`) using the complete parameterized request [Type],
 * so typed variables are resolved from the declared generic arguments rather
 * than the runtime class. With the default Gson instance, absent variables
 * and extensions are written as explicit JSON nulls.
 *
 * ## Response decoding
 *
 * [decodeResponse] preserves the neutral protocol semantics:
 * - A missing `data` key decodes to [GraphQLData.Absent], while an explicit
 *   `data: null` decodes to [GraphQLData.Present] with a null value.
 * - Error `message` is required: missing, null, or non-string messages fail
 *   with [GraphQLResponseDecodingException].
 * - Error `path` segments decode to [GraphQLPathSegment.Field] for strings
 *   and [GraphQLPathSegment.Index] for integral numbers; non-integral or
 *   non-scalar segments fail.
 * - Top-level and error `extensions` decode to [GraphQLValue.ObjectValue]
 *   with exact [java.math.BigDecimal] precision for numbers.
 * - Explicit JSON nulls for optional `errors`/`extensions` fields are treated
 *   as absent.
 *
 * ## Exception policy
 *
 * Every failure is wrapped in [GraphQLRequestEncodingException] or
 * [GraphQLResponseDecodingException] with the operation or response type
 * context; backend exceptions never leak.
 *
 * @param gson A configured [Gson] instance. Defaults to a Gson with null
 *   serialization enabled so absent request fields stay explicit.
 *
 * @see GraphQLTransportCodec
 * @see GraphQLResponseDecodingException
 */
class GsonGraphQLTransportCodec(
    private val gson: Gson = GsonBuilder().serializeNulls().create(),
) : GraphQLTransportCodec {
    override fun encodeRequest(
        request: GraphQLOperationRequest<*>,
        requestType: Type,
    ): String =
        try {
            val envelope = JsonObject()
            envelope.addProperty(QUERY, request.query)
            envelope.addProperty(OPERATION_NAME, request.operationName)
            envelope.add(
                VARIABLES,
                request.variables?.let { variables ->
                    val variablesType =
                        (requestType as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
                    if (variablesType != null) {
                        gson.toJsonTree(variables, variablesType)
                    } else {
                        gson.toJsonTree(variables)
                    }
                } ?: JsonNull.INSTANCE,
            )
            envelope.add(EXTENSIONS, request.extensions?.let(::encodeGraphQLValue) ?: JsonNull.INSTANCE)
            gson.toJson(envelope)
        } catch (cause: Exception) {
            throw GraphQLRequestEncodingException(
                operationName = request.operationName,
                requestType = requestType,
                message = "Failed to encode GraphQL request '${request.operationName}' as $requestType with Gson",
                cause = cause,
            )
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> decodeResponse(
        body: String,
        responseType: Type,
    ): T =
        try {
            val envelope = parseEnvelope(body, responseType)
            val dataType =
                responseType.responseDataType()
                    ?: throw GraphQLResponseDecodingException(
                        responseType = responseType,
                        message = "Expected a parameterized GraphQLResponse<T> type, but got: $responseType",
                    )

            val data =
                if (envelope.has(DATA)) {
                    val dataElement = envelope.get(DATA)
                    if (dataElement.isJsonNull) {
                        GraphQLData.Present<T>(null)
                    } else {
                        GraphQLData.Present(decodeData(dataElement, dataType, responseType))
                    }
                } else {
                    GraphQLData.Absent
                }

            val errors =
                envelope.get(ERRORS)
                    ?.takeUnless { it.isJsonNull }
                    ?.let { decodeErrors(it, responseType) }
                    ?: emptyList()

            val extensions =
                envelope.get(EXTENSIONS)
                    ?.takeUnless { it.isJsonNull }
                    ?.let { decodeObjectValue(it, responseType, "response 'extensions'") }

            GraphQLResponse(data = data, errors = errors, extensions = extensions) as T
        } catch (cause: GraphQLResponseDecodingException) {
            throw cause
        } catch (cause: Exception) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "Failed to decode GraphQL response as $responseType with Gson",
                cause = cause,
            )
        }

    private fun parseEnvelope(
        body: String,
        responseType: Type,
    ): JsonObject {
        val root = JsonParser.parseString(body)
        if (!root.isJsonObject) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "Expected a JSON object GraphQL response envelope, but got a ${root.javaClass.simpleName}",
            )
        }
        return root.asJsonObject
    }

    private fun Type.responseDataType(): Type? =
        (this as? ParameterizedType)
            ?.takeIf { it.rawType == GraphQLResponse::class.java }
            ?.actualTypeArguments
            ?.firstOrNull()

    private fun <T : Any> decodeData(
        dataElement: JsonElement,
        dataType: Type,
        responseType: Type,
    ): T {
        val value = gson.fromJson<T>(dataElement, dataType)
        return value
    }

    private fun decodeErrors(
        errorsElement: JsonElement,
        responseType: Type,
    ): List<GraphQLResponseError> {
        if (!errorsElement.isJsonArray) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL response 'errors' must be a JSON array",
            )
        }
        return errorsElement.asJsonArray.map { decodeError(it, responseType) }
    }

    private fun decodeError(
        errorElement: JsonElement,
        responseType: Type,
    ): GraphQLResponseError {
        if (!errorElement.isJsonObject) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error entries must be JSON objects",
            )
        }
        val errorObject = errorElement.asJsonObject
        val messageElement =
            errorObject.get(MESSAGE)
                ?: throw GraphQLResponseDecodingException(
                    responseType = responseType,
                    message = "GraphQL error is missing the required 'message' field",
                )
        if (!messageElement.isJsonPrimitive || !messageElement.asJsonPrimitive.isString) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error 'message' must be a string",
            )
        }
        val message = messageElement.asString
        val locations =
            errorObject.get(LOCATIONS)
                ?.takeUnless { it.isJsonNull }
                ?.let { decodeLocations(it, responseType) }
        val path =
            errorObject.get(PATH)
                ?.takeUnless { it.isJsonNull }
                ?.let { decodePath(it, responseType) }
        val extensions =
            errorObject.get(EXTENSIONS)
                ?.takeUnless { it.isJsonNull }
                ?.let { decodeObjectValue(it, responseType, "error 'extensions'") }
        return GraphQLResponseError(
            message = message,
            locations = locations,
            path = path,
            extensions = extensions,
        )
    }

    private fun decodeLocations(
        locationsElement: JsonElement,
        responseType: Type,
    ): List<GraphQLResponseError.Location> {
        if (!locationsElement.isJsonArray) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error 'locations' must be a JSON array",
            )
        }
        return locationsElement.asJsonArray.map { locationElement ->
            if (!locationElement.isJsonObject) {
                throw GraphQLResponseDecodingException(
                    responseType = responseType,
                    message = "GraphQL error 'locations' entries must be JSON objects",
                )
            }
            val locationObject = locationElement.asJsonObject
            GraphQLResponseError.Location(
                line = locationObject.requireInteger(LINE, responseType),
                column = locationObject.requireInteger(COLUMN, responseType),
            )
        }
    }

    private fun JsonObject.requireInteger(
        fieldName: String,
        responseType: Type,
    ): Int {
        val element =
            get(fieldName)
                ?: throw GraphQLResponseDecodingException(
                    responseType = responseType,
                    message = "GraphQL error 'locations' entry is missing the required '$fieldName' field",
                )
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error 'locations' '$fieldName' must be an integer",
            )
        }
        val decimal = element.asJsonPrimitive.asBigDecimal
        if (decimal.stripTrailingZeros().scale() > 0) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error 'locations' '$fieldName' must be an integer",
            )
        }
        return decimal.toInt()
    }

    private fun decodePath(
        pathElement: JsonElement,
        responseType: Type,
    ): List<GraphQLPathSegment> {
        if (!pathElement.isJsonArray) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error 'path' must be a JSON array",
            )
        }
        return pathElement.asJsonArray.map { segmentElement ->
            when {
                segmentElement.isJsonPrimitive && segmentElement.asJsonPrimitive.isString ->
                    GraphQLPathSegment.Field(segmentElement.asString)

                segmentElement.isJsonPrimitive && segmentElement.asJsonPrimitive.isNumber -> {
                    val decimal = segmentElement.asJsonPrimitive.asBigDecimal
                    if (decimal.stripTrailingZeros().scale() > 0) {
                        throw GraphQLResponseDecodingException(
                            responseType = responseType,
                            message = "GraphQL error 'path' index segments must be integers",
                        )
                    }
                    GraphQLPathSegment.Index(decimal.toInt())
                }

                else ->
                    throw GraphQLResponseDecodingException(
                        responseType = responseType,
                        message = "GraphQL error 'path' segments must be field strings or integer indexes",
                    )
            }
        }
    }

    private fun decodeObjectValue(
        element: JsonElement,
        responseType: Type,
        context: String,
    ): GraphQLValue.ObjectValue {
        if (!element.isJsonObject) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL $context must be a JSON object",
            )
        }
        return GraphQLValue.ObjectValue(
            fields = element.asJsonObject.entrySet().associate { (key, value) -> key to decodeGraphQLValue(value) },
        )
    }

    private fun decodeGraphQLValue(element: JsonElement): GraphQLValue =
        when {
            element.isJsonNull -> GraphQLValue.Null
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                when {
                    primitive.isBoolean -> GraphQLValue.BooleanValue(primitive.asBoolean)
                    primitive.isString -> GraphQLValue.StringValue(primitive.asString)
                    primitive.isNumber -> GraphQLValue.NumberValue(primitive.asBigDecimal)
                    else -> GraphQLValue.Null
                }
            }
            element.isJsonArray ->
                GraphQLValue.ListValue(element.asJsonArray.map(::decodeGraphQLValue))

            element.isJsonObject ->
                GraphQLValue.ObjectValue(
                    fields = element.asJsonObject.entrySet().associate { (key, value) -> key to decodeGraphQLValue(value) },
                )

            else -> GraphQLValue.Null
        }

    private fun encodeGraphQLValue(value: GraphQLValue): JsonElement =
        when (value) {
            is GraphQLValue.Null -> JsonNull.INSTANCE
            is GraphQLValue.BooleanValue -> JsonPrimitive(value.value)
            is GraphQLValue.StringValue -> JsonPrimitive(value.value)
            is GraphQLValue.NumberValue -> JsonPrimitive(value.value)
            is GraphQLValue.ListValue ->
                JsonArray().apply {
                    value.values.forEach { add(encodeGraphQLValue(it)) }
                }

            is GraphQLValue.ObjectValue ->
                JsonObject().apply {
                    value.fields.forEach { (key, fieldValue) -> add(key, encodeGraphQLValue(fieldValue)) }
                }
        }

    private companion object {
        const val QUERY = "query"
        const val OPERATION_NAME = "operationName"
        const val VARIABLES = "variables"
        const val DATA = "data"
        const val ERRORS = "errors"
        const val EXTENSIONS = "extensions"
        const val MESSAGE = "message"
        const val LOCATIONS = "locations"
        const val PATH = "path"
        const val LINE = "line"
        const val COLUMN = "column"
    }
}
