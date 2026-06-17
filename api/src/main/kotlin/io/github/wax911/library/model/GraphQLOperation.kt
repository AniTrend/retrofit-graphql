package io.github.wax911.library.model

/**
 * Represents a statically-known GraphQL operation with generated metadata.
 *
 * @param TVariables The type of variables this operation accepts, or [Unit] if none.
 */
interface GraphQLOperation<TVariables : Any> {
    /** The operation name constant (e.g. "GetCurrentUser"). */
    val name: String

    /** The full GraphQL document string (with fragments inlined). */
    val document: String

    /** The SHA-256 hex digest of the document, for APQ. */
    val sha256Hash: String
}

/**
 * Convenience interface for operations that take no variables.
 */
interface GraphQLNoVarOperation : GraphQLOperation<Unit>
