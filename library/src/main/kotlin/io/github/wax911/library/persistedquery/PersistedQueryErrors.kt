package io.github.wax911.library.persistedquery

@Deprecated(
    "Consider migrating to AutomaticPersistedQueryErrors instead",
    ReplaceWith(
        "AutomaticPersistedQueryErrors",
        "io.github.wax911.library.persisted.query.error.AutomaticPersistedQueryErrors"
    )
)
object PersistedQueryErrors {
    // server does not support Automatic Persisted Queries. The client should fallback to regular query
    const val APQ_NOT_SUPPORTED_ERROR = "PersistedQueryNotSupported"
    // the server has yet seen the query content. Client should fallback to regular query
    const val APQ_QUERY_NOT_FOUND_ERROR = "PersistedQueryNotFound"
}