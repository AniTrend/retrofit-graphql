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

package co.anitrend.retrofit.graphql.serialization

import java.lang.reflect.Type

/**
 * Base type for all failures produced by a [GraphQLTransportCodec].
 *
 * Codec implementations must wrap backend failures in [GraphQLRequestEncodingException]
 * or [GraphQLResponseDecodingException] instead of leaking backend-specific
 * exceptions, so runtime callers can rely on a single exception family.
 */
open class GraphQLTransportCodecException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Thrown when [GraphQLTransportCodec.encodeRequest] fails to serialize a request.
 *
 * Carries the operation and type context needed by runtime code to build a
 * useful error message or log entry.
 *
 * @property operationName The operation name of the failed request.
 * @property requestType The complete [Type] of the failed request, including
 *   generic arguments.
 */
class GraphQLRequestEncodingException(
    val operationName: String,
    val requestType: Type,
    message: String,
    cause: Throwable? = null,
) : GraphQLTransportCodecException(message, cause)

/**
 * Thrown when [GraphQLTransportCodec.decodeResponse] fails to deserialize a response.
 *
 * Carries the type context needed by runtime code to build a useful error
 * message or log entry.
 *
 * @property responseType The complete [Type] of the failed response,
 *   including generic arguments.
 */
class GraphQLResponseDecodingException(
    val responseType: Type,
    message: String,
    cause: Throwable? = null,
) : GraphQLTransportCodecException(message, cause)
