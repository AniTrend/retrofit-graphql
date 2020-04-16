package io.github.wax911.library.annotation.processor.fragment

import io.github.wax911.library.util.Logger

/**
 * This class will return a String containing fragment definitions. This String can then be appended to a GraphQL
 * operation in order to have all referenced fragments fully defined, before sending it to the server. This allows a
 * GraphQL operation to reference fragments, but have the definition of those fragments live outside of the file that
 * the operation lives in.
 *
 * This class also properly handles recursion. This means fragments can have references to other fragments, and it will
 * do its best to resolve everything correctly.
 */
class FragmentPatcher(
    private val defaultExtension: String,
    private val fragmentAnalyzer: FragmentAnalyzer = RegexFragmentAnalyzer()
) {
    fun includeMissingFragments(
        graphFile: String,
        graphContent: String,
        availableGraphFiles: Map<String, String>,
        aggregation: StringBuilder = StringBuilder()
    ): String {
        // Look for any missing fragment definitions in the current graph content.
        val missingFragments = fragmentAnalyzer.analyzeFragments(graphContent).filter { !it.isDefined }

        if (missingFragments.isEmpty()) {
            // Nothing to do. We can short circuit and return early.
            return aggregation.toString()
        }

        // There is at least one missing fragment definition. It may be defined in its own file though. We will do
        // our best to find and include it.
        val count = missingFragments.count()
        Logger.d(TAG, "$count missing fragments in $graphFile. Attempting to find them elsewhere.")

        missingFragments.forEach { missingFragment ->
            val includeFile = "${missingFragment.fragmentReference}$defaultExtension"
            val includeGraphContent = availableGraphFiles[includeFile]

            if (includeGraphContent != null) {
                // Found it! It, too, may have fragment references. So we need to recursively check it.
                includeMissingFragments(includeFile, includeGraphContent, availableGraphFiles, aggregation)

                // Now we can append this fragment's content.
                val isNew = !aggregation.contains(includeGraphContent.toRegex(RegexOption.LITERAL))
                if (isNew) {
                    aggregation.append("\n\n$includeGraphContent")
                }
            } else {
                // This fragment is nowhere to be found.
                Logger.e(TAG, "$graphFile references $missingFragment, but it could not be located.")
            }
        }

        Logger.d(TAG, "Patch produced for: $graphFile\n$aggregation")

        return aggregation.toString()
    }

    companion object {
        private const val TAG = "FragmentPatcher"
    }
}