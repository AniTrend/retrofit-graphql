package io.github.wax911.library.model.request

/**
 * Data for persisted queries where query content is sent as query parameters in the URL
 * instead of a json formatted body
 */
data class PersistedQueryUrlParameters(
    val extensions: String,
    val operationName: String,
    val variables: String
)