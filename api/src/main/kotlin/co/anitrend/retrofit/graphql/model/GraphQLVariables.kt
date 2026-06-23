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
 * Marker interface for generated GraphQL variable classes.
 *
 * All generated operation variable classes implement this interface,
 * allowing the runtime to treat them uniformly. Generated variable classes
 * are produced when `generateVariables = true` is set in the codegen DSL.
 *
 * @see GraphQLRequest
 * @see EmptyGraphQLVariables
 */
interface GraphQLVariables

/**
 * Sentinel type for operations that take no variables.
 * Generated operation objects for no-variable queries implement
 * [GraphQLOperation]<EmptyGraphQLVariables> via [GraphQLNoVarOperation].
 */
object EmptyGraphQLVariables : GraphQLVariables
