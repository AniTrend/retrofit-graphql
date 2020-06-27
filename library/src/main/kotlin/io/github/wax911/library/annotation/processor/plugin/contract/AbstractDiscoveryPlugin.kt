package io.github.wax911.library.annotation.processor.plugin.contract

import androidx.annotation.VisibleForTesting
import io.github.wax911.library.logger.core.AbstractLogger
import java.io.InputStream

/**
 * A discovery plugin that defines a contract to handle multiple sources
 */
abstract class AbstractDiscoveryPlugin<S: Any>(
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