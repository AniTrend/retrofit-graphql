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

package io.github.wax911.library.model

import io.github.wax911.library.model.body.GraphContainer

/**
 * Returns the non-null [GraphContainer.data] or throws [GraphQLResponseException]
 * if the response contains errors.
 *
 * @throws GraphQLResponseException when [GraphContainer.data] is null.
 */
fun <T> GraphContainer<T>.requireData(): T = data ?: throw GraphQLResponseException(errors = errors)

/**
 * Transforms [GraphContainer.data] with [transform] if present, or returns null.
 *
 * @param transform A mapping function applied to non-null data.
 * @return The transformed value, or null when data is null.
 */
inline fun <T, R> GraphContainer<T>.mapData(transform: (T) -> R): R? = data?.let(transform)
