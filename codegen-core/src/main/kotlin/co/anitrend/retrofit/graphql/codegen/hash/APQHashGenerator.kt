/**
 * Copyright 2026 AniTrend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
