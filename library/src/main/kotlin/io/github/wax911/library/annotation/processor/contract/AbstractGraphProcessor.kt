package io.github.wax911.library.annotation.processor.contract

import io.github.wax911.library.annotation.GraphQuery
import io.github.wax911.library.annotation.processor.fragment.FragmentPatcher
import io.github.wax911.library.logger.core.AbstractLogger

/**
 * GraphQL annotation processor contract
 *
 * @property defaultExtension The default extension to use for looking up graphql files
 * @property defaultDirectory The target directory to look in within `assetManager`
 * @property logger A custom logger facade which should be used
 * @property fragmentPatcher An optional fragment patcher
 *
 * @see FragmentPatcher
 * @see AbstractLogger
 */
abstract class AbstractGraphProcessor {

    internal abstract val defaultExtension: String
    internal abstract val defaultDirectory: String
    internal abstract val logger: AbstractLogger
    internal abstract val fragmentPatcher: FragmentPatcher

    /**
     * Returns a pair of query or mutation name and graphql contents
     */
    abstract val graphFiles: Map<String, String>

    /**
     * Finds graphql content matching the value provided by [GraphQuery]
     *
     * @param annotations A collection of method annotation from an ongoing request
     *
     * @return GraphQL query in the form of [String] or null if the request method was not
     * annotated with [GraphQuery] or if the no such file could be found
     */
    abstract fun getQuery(annotations: Array<out Annotation>): String?

    /**
     * Patch any query with fragment references, that don't already have the
     * fragment defined with the query.
     */
    protected abstract fun patchQueries()
}