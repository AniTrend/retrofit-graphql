package io.github.wax911.library.helpers

import io.github.wax911.library.annotation.processor.plugin.contract.AbstractDiscoveryPlugin
import io.github.wax911.library.logger.core.AbstractLogger
import java.io.File
import java.io.InputStream
import java.net.URL

class ResourcesDiscoveryPlugin(
    self: Class<ResourcesDiscoveryPlugin> = ResourcesDiscoveryPlugin::class.java
) : AbstractDiscoveryPlugin<Class<ResourcesDiscoveryPlugin>>(self) {
    override val targetPath: String = "graphql"
    override val targetExtension: String = ".graphql"

    private val temporaryMap = HashMap<String, String>()

    private fun getResourceFolderFiles(url: URL): List<File> {
        val path = url.path
        val sequenceFiles = File(path).walk()
        return sequenceFiles.filter {
            val fileName = it.name
            fileName.endsWith(targetExtension)
        }.toList()
    }

    /**
     * Reads the file contents for a given [inputStream]
     */
    override fun resolveContents(
        inputStream: InputStream,
        logger: AbstractLogger
    ) = buildString {
        inputStream.bufferedReader().useLines { sequence ->
            sequence.forEach(::append)
        }
    }

    /**
     * Invokes initial discovery using [source] to produce a map
     */
    override fun startDiscovery(logger: AbstractLogger): Map<String, String> {
        val urlPath = source.getResource(targetPath)
        if (urlPath != null) {
            val files = getResourceFolderFiles(urlPath)
            files.forEach { file ->
                val contents = resolveContents(file.inputStream(), logger)
                assert(contents.isNotEmpty()) {
                    "Contents of a file that needs to be read should never be empty"
                }
                temporaryMap[file.name] = contents
            }
        }
        return temporaryMap
    }
}