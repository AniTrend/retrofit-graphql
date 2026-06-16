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

/**
 * An implementation of FragmentAnalyzer that simply uses regular expressions to find fragment references, and whether
 * they are defined with the query.
 *
 * @author eschlenz
 */
class RegexFragmentAnalyzer : FragmentAnalyzer {
    override fun analyzeFragments(graphqlContent: String): Set<FragmentAnalysis> {
        val fragmentReferences = GraphRegexUtil.findFragmentReferences(graphqlContent)
        val fragmentDefinitions = GraphRegexUtil.findFragmentDefinitions(graphqlContent)

        return fragmentReferences.map {
            FragmentAnalysis(
                fragmentReference = it,
                isDefined = fragmentDefinitions.contains(it),
            )
        }.toSet()
    }
}
