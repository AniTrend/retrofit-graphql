package io.github.wax911.library.persistedquery

object PersistedQueryErrors {
    // server does not support Automatic Persisted Queries. The client should fallback to regular query
    const val APQ_NOT_SUPPORTED_ERROR = "PersistedQueryNotSupported"
    // the server has yet seen the query content. Client should fallback to regular query
    const val APQ_QUERY_NOT_FOUND_ERROR = "PersistedQueryNotFound"
}