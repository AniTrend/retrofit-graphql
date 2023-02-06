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

package io.github.wax911.library.annotation.processor

import android.content.res.AssetManager
import io.github.wax911.library.annotation.GraphQuery
import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.annotation.processor.fragment.FragmentPatcher
import io.github.wax911.library.annotation.processor.fragment.GraphRegexUtil
import io.github.wax911.library.annotation.processor.plugin.AssetManagerDiscoveryPlugin
import io.github.wax911.library.annotation.processor.plugin.contract.AbstractDiscoveryPlugin
import io.github.wax911.library.logger.DefaultGraphLogger
import io.github.wax911.library.logger.core.AbstractLogger
import java.io.IOException

/**
 * GraphQL annotation processor that resolves `graphql` queries, mutations and fragments
 * by a given file name declared in [GraphQuery]
 *
 * @param discoveryPlugin A plugin which handles the discovery of .graphql files
 * in this case we will be using [AssetManagerDiscoveryPlugin]
 */
class GraphProcessor(
    discoveryPlugin: AbstractDiscoveryPlugin<*>,
    override val logger: AbstractLogger = DefaultGraphLogger(),
    override val fragmentPatcher: FragmentPatcher = FragmentPatcher(
        defaultExtension = discoveryPlugin.targetExtension,
        logger = logger
    )
) : AbstractGraphProcessor() {
    override val defaultExtension: String = discoveryPlugin.targetExtension
    override val defaultDirectory: String = discoveryPlugin.targetPath

    private val _graphFiles: MutableMap<String, String> = HashMap()

    /**
     * Returns a pair of query or mutation name and graphql contents
     */
    override val graphFiles: Map<String, String> get() = _graphFiles

    init {
        synchronized(lock) {
            val currentThread = Thread.currentThread().name
            logger.v(TAG, "$currentThread: has obtained a synchronized lock on the object")
            if (_graphFiles.isEmpty()) {
                logger.v(TAG, "$currentThread: is initializing query files")
                createGraphQLMap(discoveryPlugin)
                logger.v(TAG, "$currentThread: has completed initializing all graphql files")
                logger.v(TAG, "$currentThread: discovered `${_graphFiles.size}` graphql files")
            } else
                logger.v(TAG, "$currentThread: skipping discovery | cache is not empty -> size: ${_graphFiles.size}")
        }
    }

    /**
     * Finds graphql content matching the value provided by [GraphQuery]
     *
     * @param annotations A collection of method annotation from an ongoing request
     *
     * @return GraphQL query in the form of [String] or null if the request method was not
     * annotated with [GraphQuery] or if the no such file could be found
     */
    @Synchronized override fun getQuery(annotations: Array<out Annotation>): String? {
        val graphQuery = annotations.filterIsInstance(
            GraphQuery::class.java
        ).firstOrNull()

        if (graphQuery != null) {
            val fileName = "${graphQuery.value}$defaultExtension"
            logger.d(TAG, "Looking up `$defaultExtension` file matching: $fileName")
            if (_graphFiles.containsKey(fileName))
                return _graphFiles[fileName]
            logger.e(TAG, "The requested query annotated with value: ${graphQuery.value} could not be found!")
            logger.e(TAG, "Discovered GraphQL files with the size of -> ${_graphFiles.size}")
        }
        return null
    }

    /**
     * Patch any query with fragment references, that don't already have the
     * fragment defined with the query.
     */
    @Synchronized override fun patchQueries() {
        _graphFiles.entries
            .filter { entry ->
                GraphRegexUtil.containsAnOperation(entry.value)
            }
            .forEach { entry ->
                val patch = fragmentPatcher.includeMissingFragments(
                    entry.key, entry.value, _graphFiles
                )
                if (patch.isNotEmpty())
                    entry.setValue("${entry.value}$patch")
            }
    }

    /**
     * Recursively discovers graphql files for a given [path] that resides in [assetManager]
     * with an extension matching [defaultExtension]
     *
     * @param discoveryPlugin Root path used for discovery
     */
    @Synchronized private fun createGraphQLMap(discoveryPlugin: AbstractDiscoveryPlugin<*>) {
        try {
            val graphs = discoveryPlugin.startDiscovery(logger)
            if (_graphFiles.isNotEmpty())
                _graphFiles.clear()
            _graphFiles.putAll(graphs)
            patchQueries()
        } catch (e: IOException) {
            logger.e(TAG, "Error encounter while searching for .graphql files", e)
        }
    }

    companion object {

        private val TAG = GraphProcessor::class.java.simpleName

        @Volatile
        private var instance: GraphProcessor? = null
        private val lock = Any()

        /**
         * Return a single instance of the configured graph processor
         *
         * @assetManager The [AssetManager] which will be used to look for graphql files
         * @param logger Custom logger facade
         */
        @Deprecated(
            message = "Consider managing the singleton lifecycle of GraphProcessor on your " +
                "own, with something like Dagger, Hilt or even Koin",
            replaceWith = ReplaceWith(
                "GraphProcessor(assetManager)",
                "io.github.wax911.library.annotation.processor.GraphProcessor"
            ),
            level = DeprecationLevel.ERROR
        )
        @JvmOverloads
        fun getInstance(
            assetManager: AssetManager,
            logger: AbstractLogger = DefaultGraphLogger()
        ): GraphProcessor {
            val singleton = instance
            if (singleton != null)
                return singleton

            return synchronized(lock) {
                val init = instance
                if (init != null)
                    init
                else {
                    val created = GraphProcessor(
                        discoveryPlugin = AssetManagerDiscoveryPlugin(
                            assetManager
                        ),
                        logger = logger
                    )
                    instance = created
                    created
                }
            }
        }
    }
}
