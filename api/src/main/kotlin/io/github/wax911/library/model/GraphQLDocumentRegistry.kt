package io.github.wax911.library.model

/**
 * A registry that provides GraphQL document text and APQ hash for a given operation name.
 *
 * Implementations can be generated at build time (e.g., [GeneratedGraphQLRegistry]) or
 * provided manually by the consumer.
 */
public interface GraphQLDocumentRegistry {

    /**
     * Returns the full GraphQL document text for the given operation name,
     * or null if the operation is not registered.
     */
    public fun document(operationName: String): String?

    /**
     * Returns the SHA-256 hash of the operation's document text,
     * or null if the operation is not registered.
     */
    public fun hash(operationName: String): String?
}
