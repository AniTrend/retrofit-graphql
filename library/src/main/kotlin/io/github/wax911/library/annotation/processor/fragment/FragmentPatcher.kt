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

package io.github.wax911.library.annotation.processor.fragment

import io.github.wax911.library.logger.core.AbstractLogger

/**
 * This class will return a String containing fragment definitions. This String can then be appended to a GraphQL
 * operation in order to have all referenced fragments fully defined, before sending it to the server. This allows a
 * GraphQL operation to reference fragments, but have the definition of those fragments live outside of the file that
 * the operation lives in.
 *
 * This class also properly handles recursion. This means fragments can have references to other fragments, and it will
 * do its best to resolve everything correctly.
 *
 * @author eschlenz
 */
class FragmentPatcher(
    private val defaultExtension: String,
    private val fragmentAnalyzer: FragmentAnalyzer = RegexFragmentAnalyzer(),
    private val logger: AbstractLogger
) {
    fun includeMissingFragments(
        graphFile: String,
        graphContent: String,
        availableGraphFiles: Map<String, String>,
        aggregation: StringBuilder = StringBuilder()
    ): String {
        // Look for any missing fragment definitions in the current graph content.
        val missingFragments = fragmentAnalyzer
            .analyzeFragments(graphContent)
            .filter { !it.isDefined }

        if (missingFragments.isEmpty()) {
            // Nothing to do. We can short circuit and return early.
            return aggregation.toString()
        }

        // There is at least one missing fragment definition. It may be defined in its own file though. We will do
        // our best to find and include it.
        val count = missingFragments.count()
        logger.v(TAG, "$count missing fragments in $graphFile. Attempting to find them elsewhere.")

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
                logger.w(
                    TAG,
                    "$graphFile references $missingFragment, but it could not be located."
                )
            }
        }

        logger.v(TAG, "Patch produced for: $graphFile\n$aggregation")

        return aggregation.toString()
    }

    companion object {
        private val TAG = FragmentPatcher::class.java.simpleName
    }
}
