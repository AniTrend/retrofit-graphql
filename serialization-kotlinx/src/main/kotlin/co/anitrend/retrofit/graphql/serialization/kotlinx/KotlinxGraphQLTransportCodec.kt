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

package co.anitrend.retrofit.graphql.serialization.kotlinx

import co.anitrend.retrofit.graphql.model.GraphQLData
import co.anitrend.retrofit.graphql.model.GraphQLPathSegment
import co.anitrend.retrofit.graphql.model.GraphQLResponse
import co.anitrend.retrofit.graphql.model.GraphQLResponseError
import co.anitrend.retrofit.graphql.model.GraphQLValue
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import co.anitrend.retrofit.graphql.serialization.GraphQLRequestEncodingException
import co.anitrend.retrofit.graphql.serialization.GraphQLResponseDecodingException
import co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * kotlinx.serialization-backed implementation of [GraphQLTransportCodec].
 *
 * [KotlinxGraphQLTransportCodec] serializes [GraphQLOperationRequest]
 * envelopes and deserializes [GraphQLResponse] envelopes with kotlinx
 * serialization only inside this module. It is the neutral counterpart of
 * the legacy [co.anitrend.retrofit.graphql.serialization.kotlinx.KotlinxGraphQLJson]
 * (which moved to `:compat` with the deprecated `GraphConverter` flow) and
 * never routes through the generic
 * [co.anitrend.retrofit.graphql.model.GraphQLJson] seam.
 *
 * ## Request encoding
 *
 * [encodeRequest] writes the neutral envelope (`query`, `operationName`,
 * `variables`, `extensions`). Typed variables are encoded through the
 * serializer resolved from the first generic argument of the complete
 * parameterized request [Type], falling back to the runtime class of the
 * variables when the type is not parameterized. Absent variables and
 * extensions are written as explicit JSON nulls.
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
 * @param json A configured [Json] instance. Defaults to [Json] with
 *   [Json.ignoreUnknownKeys] enabled so fields not present in the generated
 *   data classes are silently skipped.
 *
 * @see GraphQLTransportCodec
 * @see GraphQLResponseDecodingException
 */
class KotlinxGraphQLTransportCodec(
    private val json: Json = Json { ignoreUnknownKeys = true },
) : GraphQLTransportCodec {
    override fun encodeRequest(
        request: GraphQLOperationRequest<*>,
        requestType: Type,
    ): String =
        try {
            val variablesElement =
                request.variables?.let { variables ->
                    val variablesType =
                        (requestType as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
                    val serializer = resolveSerializer(variablesType, variables::class.java)
                    json.encodeToJsonElement(serializer, variables)
                } ?: JsonNull

            val envelope =
                buildJsonObject {
                    put("query", request.query)
                    put("operationName", request.operationName)
                    put("variables", variablesElement)
                    put("extensions", request.extensions?.let(::encodeGraphQLValue) ?: JsonNull)
                }
            envelope.toString()
        } catch (cause: GraphQLRequestEncodingException) {
            throw cause
        } catch (cause: Exception) {
            throw GraphQLRequestEncodingException(
                operationName = request.operationName,
                requestType = requestType,
                message = "Failed to encode GraphQL request '${request.operationName}' as $requestType with kotlinx.serialization",
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
                if (envelope.containsKey(DATA)) {
                    val dataElement = envelope.getValue(DATA)
                    if (dataElement is JsonNull) {
                        GraphQLData.Present<T>(null)
                    } else {
                        GraphQLData.Present(decodeData(dataElement, dataType, responseType))
                    }
                } else {
                    GraphQLData.Absent
                }

            val errors =
                envelope[ERRORS]
                    ?.takeUnless { it is JsonNull }
                    ?.let { decodeErrors(it, responseType) }
                    ?: emptyList()

            val extensions =
                envelope[EXTENSIONS]
                    ?.takeUnless { it is JsonNull }
                    ?.let { decodeObjectValue(it, responseType, "response 'extensions'") }

            GraphQLResponse(data = data, errors = errors, extensions = extensions) as T
        } catch (cause: GraphQLResponseDecodingException) {
            throw cause
        } catch (cause: Exception) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "Failed to decode GraphQL response as $responseType with kotlinx.serialization",
                cause = cause,
            )
        }

    private fun parseEnvelope(
        body: String,
        responseType: Type,
    ): JsonObject {
        val root = json.parseToJsonElement(body)
        if (root !is JsonObject) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "Expected a JSON object GraphQL response envelope, but got a ${root::class.simpleName}",
            )
        }
        return root
    }

    private fun Type.responseDataType(): Type? =
        (this as? ParameterizedType)
            ?.takeIf { it.rawType == GraphQLResponse::class.java }
            ?.actualTypeArguments
            ?.firstOrNull()

    private fun resolveSerializer(
        type: Type?,
        fallbackClass: Class<*>,
    ): KSerializer<Any> {
        val resolved = type ?: return json.serializersModule.serializer(fallbackClass)
        return json.serializersModule.serializer(resolved)
    }

    private fun <T : Any> decodeData(
        dataElement: JsonElement,
        dataType: Type,
        responseType: Type,
    ): T {
        val serializer = json.serializersModule.serializer(dataType)
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromJsonElement(serializer, dataElement) as T
    }

    private fun decodeErrors(
        errorsElement: JsonElement,
        responseType: Type,
    ): List<GraphQLResponseError> {
        if (errorsElement !is JsonArray) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL response 'errors' must be a JSON array",
            )
        }
        return errorsElement.map { decodeError(it, responseType) }
    }

    private fun decodeError(
        errorElement: JsonElement,
        responseType: Type,
    ): GraphQLResponseError {
        if (errorElement !is JsonObject) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error entries must be JSON objects",
            )
        }
        val messageElement =
            errorElement[MESSAGE]
                ?: throw GraphQLResponseDecodingException(
                    responseType = responseType,
                    message = "GraphQL error is missing the required 'message' field",
                )
        if (messageElement !is JsonPrimitive || !messageElement.isString) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error 'message' must be a string",
            )
        }
        val message = messageElement.content
        val locations =
            errorElement[LOCATIONS]
                ?.takeUnless { it is JsonNull }
                ?.let { decodeLocations(it, responseType) }
        val path =
            errorElement[PATH]
                ?.takeUnless { it is JsonNull }
                ?.let { decodePath(it, responseType) }
        val extensions =
            errorElement[EXTENSIONS]
                ?.takeUnless { it is JsonNull }
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
        if (locationsElement !is JsonArray) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error 'locations' must be a JSON array",
            )
        }
        return locationsElement.map { locationElement ->
            if (locationElement !is JsonObject) {
                throw GraphQLResponseDecodingException(
                    responseType = responseType,
                    message = "GraphQL error 'locations' entries must be JSON objects",
                )
            }
            GraphQLResponseError.Location(
                line = locationElement.requireInteger(LINE, responseType),
                column = locationElement.requireInteger(COLUMN, responseType),
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
        if (element !is JsonPrimitive || element is JsonNull) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error 'locations' '$fieldName' must be an integer",
            )
        }
        val decimal =
            element.content.toBigDecimalOrNull()
                ?: throw GraphQLResponseDecodingException(
                    responseType = responseType,
                    message = "GraphQL error 'locations' '$fieldName' must be an integer",
                )
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
        if (pathElement !is JsonArray) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL error 'path' must be a JSON array",
            )
        }
        return pathElement.map { segmentElement ->
            when {
                segmentElement is JsonPrimitive && segmentElement.isString ->
                    GraphQLPathSegment.Field(segmentElement.content)

                segmentElement is JsonPrimitive -> {
                    val decimal =
                        segmentElement.content.toBigDecimalOrNull()
                            ?: throw GraphQLResponseDecodingException(
                                responseType = responseType,
                                message = "GraphQL error 'path' segments must be field strings or integer indexes",
                            )
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
        if (element !is JsonObject) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "GraphQL $context must be a JSON object",
            )
        }
        return GraphQLValue.ObjectValue(
            fields = element.mapValues { (_, value) -> decodeGraphQLValue(value) },
        )
    }

    private fun decodeGraphQLValue(element: JsonElement): GraphQLValue =
        when (element) {
            is JsonNull -> GraphQLValue.Null
            is JsonObject ->
                GraphQLValue.ObjectValue(
                    fields = element.mapValues { (_, value) -> decodeGraphQLValue(value) },
                )

            is JsonArray -> GraphQLValue.ListValue(element.map(::decodeGraphQLValue))
            is JsonPrimitive ->
                when {
                    element.isString -> GraphQLValue.StringValue(element.content)
                    element.content == "true" -> GraphQLValue.BooleanValue(true)
                    element.content == "false" -> GraphQLValue.BooleanValue(false)
                    else -> GraphQLValue.NumberValue(element.content.toBigDecimal())
                }
        }

    private fun encodeGraphQLValue(value: GraphQLValue): JsonElement =
        when (value) {
            is GraphQLValue.Null -> JsonNull
            is GraphQLValue.BooleanValue -> JsonPrimitive(value.value)
            is GraphQLValue.StringValue -> JsonPrimitive(value.value)
            is GraphQLValue.NumberValue -> JsonPrimitive(value.value)
            is GraphQLValue.ListValue -> JsonArray(value.values.map(::encodeGraphQLValue))
            is GraphQLValue.ObjectValue ->
                JsonObject(
                    value.fields.mapValues { (_, fieldValue) -> encodeGraphQLValue(fieldValue) },
                )
        }

    private companion object {
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
