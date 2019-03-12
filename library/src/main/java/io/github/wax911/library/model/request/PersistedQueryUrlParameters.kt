package io.github.wax911.library.model.request

data class PersistedQueryUrlParameters(
        val extensions: String,
        val operationName: String,
        val variables: String
)