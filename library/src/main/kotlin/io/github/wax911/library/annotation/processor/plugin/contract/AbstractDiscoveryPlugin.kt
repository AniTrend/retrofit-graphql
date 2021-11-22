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

package io.github.wax911.library.annotation.processor.plugin.contract

import androidx.annotation.VisibleForTesting
import io.github.wax911.library.logger.core.AbstractLogger
import java.io.InputStream

/**
 * A discovery plugin that defines a contract to handle multiple sources
 */
abstract class AbstractDiscoveryPlugin<S : Any>(
    protected val source: S
) {

    internal abstract val targetPath: String
    internal abstract val targetExtension: String

    /**
     * Reads the file contents for a given [inputStream]
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected abstract fun resolveContents(inputStream: InputStream, logger: AbstractLogger): String

    /**
     * Invokes initial discovery using [source] to produce a map
     */
    abstract fun startDiscovery(logger: AbstractLogger): Map<String, String>
}
