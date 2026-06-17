package io.github.wax911.library.model

import io.github.wax911.library.model.request.PersistedQuery

/**
 * A typed GraphQL request payload.
 *
 * @param TVariables The type of variables, or [Unit] for operations without variables.
 * @property query The full GraphQL document string.
 * @property operationName The operation name.
 * @property variables The operation variables, or null if there are none.
 * @property extensions Optional extensions map (e.g. persistedQuery).
 */
data class GraphQLRequest<TVariables : Any>(
    val query: String,
    val operationName: String,
    val variables: TVariables? = null,
    val extensions: Map<String, Any?> = emptyMap(),
) {

    /**
     * Returns a copy of this request with a persisted query extension added.
     */
    fun withPersistedQuery(
        sha256Hash: String,
        version: Int = 1,
    ): GraphQLRequest<TVariables> = copy(
        extensions = extensions + mapOf(
            "persistedQuery" to PersistedQuery(
                sha256Hash = sha256Hash,
                version = version,
            ),
        ),
    )
}
