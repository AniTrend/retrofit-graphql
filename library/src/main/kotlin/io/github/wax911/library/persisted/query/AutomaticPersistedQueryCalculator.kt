/**
 * Copyright 2021 AniTrend
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

package io.github.wax911.library.persisted.query

import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.logger.core.AbstractLogger
import io.github.wax911.library.persisted.contract.IAutomaticPersistedQuery
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Utility class for calculating SHA256 hashes based off of the `.graphql` files held in memory by
 * [AbstractGraphProcessor]
 *
 * @param processor A self managed singleton instance of [AbstractGraphProcessor]
 *
 * @author Krillsson
 */
class AutomaticPersistedQueryCalculator(
    private val processor: AbstractGraphProcessor,
    private val logger: AbstractLogger = processor.logger
) : IAutomaticPersistedQuery {

    private val apqHashes: MutableMap<String, String> = HashMap()

    private fun hashOfQuery(query: String): String {
        val md = MessageDigest.getInstance(ALGORITHM)
        md.update(query.toByteArray())
        val digest = md.digest()
        return String.format("%064x", BigInteger(1, digest))
    }

    private fun createAndStoreHash(queryName: String, fileKey: String): String? {
        logger.d(TAG, "Creating APQ hash for $queryName")
        return if (processor.graphFiles.containsKey(fileKey)) {
            val hashOfQuery = hashOfQuery(processor.graphFiles.getValue(fileKey))
            logger.v(TAG, "Created hash for query $fileKey -> $hashOfQuery")
            apqHashes[fileKey] = hashOfQuery
            hashOfQuery
        } else {
            logger.e(
                TAG,
                "Current size of discovered graphql files -> ${processor.graphFiles.size}",
                Throwable("The request query $fileKey could not be found!")
            )
            null
        }
    }

    /**
     * Calculates the automated persisted query hash for a given [queryName]
     *
     * @return Hash or null if a query with the name [queryName] could not be found
     */
    override fun getOrCreateAPQHash(queryName: String): String? {
        val fileKey = "$queryName${processor.defaultExtension}"
        return if (apqHashes.containsKey(fileKey)) apqHashes[fileKey]
        else createAndStoreHash(queryName, fileKey)
    }

    companion object {
        private val TAG = AutomaticPersistedQueryCalculator::class.java.simpleName
        private const val ALGORITHM = "SHA-256"
    }
}
