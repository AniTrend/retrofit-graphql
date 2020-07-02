package io.github.wax911.library.persisted.query.error

/**
 * Possible persisted query errors
 *
 * @author Krillsson
 */
object AutomaticPersistedQueryErrors {
    /**
     * When the server does not support Automatic Persisted Queries.
     * The client should fallback to regular query
     */
    const val APQ_NOT_SUPPORTED_ERROR = "PersistedQueryNotSupported"

    /**
     * When the server has yet seen the query content.
     * The client should fallback to regular query
     */
    const val APQ_QUERY_NOT_FOUND_ERROR = "PersistedQueryNotFound"
}