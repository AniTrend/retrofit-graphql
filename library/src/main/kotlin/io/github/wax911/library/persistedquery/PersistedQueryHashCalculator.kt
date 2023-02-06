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

package io.github.wax911.library.persistedquery

import android.content.Context
import io.github.wax911.library.annotation.processor.GraphProcessor
import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.annotation.processor.plugin.AssetManagerDiscoveryPlugin
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Utility class for calculating SHA256 hashes based off of the .graphql files held in memory by
 * [io.github.wax911.library.annotation.processor.GraphProcessor]
 *
 * @param context A context used to obtain an instance of [GraphProcessor]
 */
@Deprecated(
    "Consider migrating to AutomaticPersistedQueryCalculator instead",
    ReplaceWith(
        "AutomaticPersistedQueryCalculator",
        "io.github.wax911.library.persisted.query.AutomaticPersistedQueryCalculator"
    )
)
class PersistedQueryHashCalculator(context: Context) {

    private val apqHashes: MutableMap<String, String> by lazy {
        HashMap<String, String>()
    }

    private val graphProcessor: AbstractGraphProcessor by lazy {
        GraphProcessor(AssetManagerDiscoveryPlugin(context.assets))
    }

    fun getOrCreateAPQHash(queryName: String): String? {
        val fileKey = "$queryName${graphProcessor.defaultExtension}"
        return if (apqHashes.containsKey(fileKey)) {
            apqHashes[fileKey]
        } else {
            createAndStoreHash(queryName, fileKey)
        }
    }

    private fun createAndStoreHash(queryName: String, fileKey: String): String? {
        graphProcessor.logger.d(TAG, "Creating hash for $queryName")
        return if (graphProcessor.graphFiles.containsKey(fileKey)) {
            val hashOfQuery = hashOfQuery(graphProcessor.graphFiles.getValue(fileKey))
            graphProcessor.logger.d(TAG, "Created ")
            apqHashes[fileKey] = hashOfQuery
            hashOfQuery
        } else {
            graphProcessor.logger.e(TAG, "The request query $fileKey could not be found!")
            graphProcessor.logger.e(TAG, "Current size of graphFiles -> size: ${graphProcessor.graphFiles.size}")
            null
        }
    }

    private fun hashOfQuery(query: String): String {
        val md = MessageDigest.getInstance(sha256Algorithm)
        md.update(query.toByteArray())
        val digest = md.digest()
        return String.format("%064x", BigInteger(1, digest))
    }

    companion object {
        private val TAG = PersistedQueryHashCalculator::class.java.simpleName
        private const val sha256Algorithm = "SHA-256"
    }
}
