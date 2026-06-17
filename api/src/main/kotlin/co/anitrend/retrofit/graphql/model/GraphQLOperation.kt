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

package co.anitrend.retrofit.graphql.model

/**
 * Represents a statically-known GraphQL operation with generated metadata.
 *
 * @param TVariables The type of variables this operation accepts, or [Unit] if none.
 */
interface GraphQLOperation<TVariables : GraphQLVariables> {
    /** The operation name constant (e.g. "GetCurrentUser"). */
    val name: String

    /** The full GraphQL document string (with fragments inlined). */
    val document: String

    /** The SHA-256 hex digest of the document, for APQ. */
    val sha256Hash: String
}

/**
 * Convenience interface for operations that take no variables.
 */
interface GraphQLNoVarOperation : GraphQLOperation<EmptyGraphQLVariables>
