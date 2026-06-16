package co.anitrend.retrofit.graphql.codegen.hash

import java.security.MessageDigest

/**
 * Generates SHA-256 hashes for GraphQL document strings.
 *
 * These hashes are used for Automatic Persisted Queries (APQ).
 * The hash must be computed from the exact document text that will be
 * sent over the wire (after fragment flattening).
 */
object APQHashGenerator {

    /**
     * Computes the SHA-256 hex digest of the given document text.
     */
    fun hash(document: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(document.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
