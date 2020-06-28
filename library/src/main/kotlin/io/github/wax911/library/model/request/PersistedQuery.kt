package io.github.wax911.library.model.request

/**
 * Contents of a PersistedQuery extension inside a QueryContainer
 *
 * @see QueryContainerBuilder.putPersistedQueryHash
 * @see QueryContainerBuilder.putExtension
 */
data class PersistedQuery(
    val sha256Hash: String,
    val version: Int
)