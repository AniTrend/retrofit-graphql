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

package io.github.wax911.library.annotation.processor.plugin

import android.content.res.AssetManager
import io.github.wax911.library.annotation.processor.plugin.contract.AbstractDiscoveryPlugin
import io.github.wax911.library.logger.core.AbstractLogger
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Discovery plugin that looks for resources with-in assets
 *
 * @param targetPath Directory to search in
 * @param targetExtension File extension to look for
 */
class AssetManagerDiscoveryPlugin(
    assetManager: AssetManager,
    override val targetPath: String = "graphql",
    override val targetExtension: String = ".graphql",
) : AbstractDiscoveryPlugin<AssetManager>(assetManager) {
    private val temporaryMap = HashMap<String, String>()

    private fun AssetManager.findRecursively(
        path: String,
        logger: AbstractLogger,
    ) {
        val paths = list(path)
        if (paths.isNullOrEmpty()) {
            logger.w(TAG, "Assets do not contain any files/folders in the path -> $path")
        }

        for (item in paths!!) {
            val absolute = "$path/$item"
            if (item.endsWith(targetExtension)) {
                temporaryMap[item] = resolveContents(open(absolute), logger)
            } else {
                findRecursively(absolute, logger)
            }
        }
    }

    /**
     * Reads the file contents for a given [inputStream]
     */
    override fun resolveContents(
        inputStream: InputStream,
        logger: AbstractLogger,
    ): String {
        val queryBuffer = StringBuilder()
        try {
            InputStreamReader(inputStream).useLines { sequence ->
                sequence.forEach { queryBuffer.append(it) }
            }
        } catch (e: IOException) {
            logger.e(TAG, "Failed to read from supplied InputStream", e)
        }

        return queryBuffer.toString()
    }

    /**
     * Invokes initial discovery using [source] to produce a map
     */
    override fun startDiscovery(logger: AbstractLogger): Map<String, String> {
        source.findRecursively(targetPath, logger)
        return temporaryMap
    }

    companion object {
        private val TAG = AssetManagerDiscoveryPlugin::class.java.simpleName
    }
}
