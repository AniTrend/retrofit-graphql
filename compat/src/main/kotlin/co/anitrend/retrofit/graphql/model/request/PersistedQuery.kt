/**
 * Copyright 2021 AniTrend
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

package co.anitrend.retrofit.graphql.model.request

import kotlinx.serialization.Serializable

/**
 * Contents of a PersistedQuery extension inside a QueryContainer
 *
 * **Legacy**: part of the deprecated [QueryContainerBuilder] flow in `:compat`.
 * New code should use [co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest.withPersistedQuery]
 * on the backend-neutral [co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest].
 *
 * @see QueryContainerBuilder.putPersistedQueryHash
 * @see QueryContainerBuilder.putExtension
 */
@Deprecated(
    "Legacy APQ extension payload of the QueryContainer flow. " +
        "Use GraphQLOperationRequest.withPersistedQuery on the neutral request contract instead.",
)
@Serializable
data class PersistedQuery(
    val sha256Hash: String,
    val version: Int,
)
