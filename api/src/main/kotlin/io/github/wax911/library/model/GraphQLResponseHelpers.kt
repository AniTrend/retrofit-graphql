package io.github.wax911.library.model

import io.github.wax911.library.model.body.GraphContainer

/**
 * Returns the non-null [GraphContainer.data] or throws [GraphQLResponseException]
 * if the response contains errors.
 *
 * @throws GraphQLResponseException when [GraphContainer.data] is null.
 */
fun <T> GraphContainer<T>.requireData(): T =
    data ?: throw GraphQLResponseException(errors = errors)

/**
 * Transforms [GraphContainer.data] with [transform] if present, or returns null.
 *
 * @param transform A mapping function applied to non-null data.
 * @return The transformed value, or null when data is null.
 */
inline fun <T, R> GraphContainer<T>.mapData(
    transform: (T) -> R,
): R? = data?.let(transform)
