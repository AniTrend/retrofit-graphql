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
