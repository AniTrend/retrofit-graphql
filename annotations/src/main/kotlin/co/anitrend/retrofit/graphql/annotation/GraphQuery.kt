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

package co.anitrend.retrofit.graphql.annotation

/**
 * Marks a Retrofit interface method as a GraphQL operation.
 *
 * The annotation value must match the name of a `.graphql` file (without extension)
 * in the configured assets directory (default: `graphql/`). At runtime, the
 * [GraphProcessor][co.anitrend.retrofit.graphql.annotation.processor.GraphProcessor]
 * discovers the file and injects its contents into the request body.
 *
 * Example:
 * ```kotlin
 * @POST("/graphql")
 * @GraphQuery("GetMarketPlaceApps")
 * suspend fun getMarketPlaceApps(@Body builder: QueryContainerBuilder): Response<GraphContainer<MarketPlaceListings>>
 * ```
 *
 * @param value The operation name, matching a `.graphql` file in assets (without extension).
 * @see co.anitrend.retrofit.graphql.model.GraphQLRequest
 * @see co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class GraphQuery(val value: String = "")
