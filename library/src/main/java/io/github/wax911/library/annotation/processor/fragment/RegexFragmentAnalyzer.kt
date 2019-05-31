package io.github.wax911.library.annotation.processor.fragment

/**
 * An implementation of FragmentAnalyzer that simply uses regular expressions to find fragment references, and whether
 * they are defined with the query.
 */
class RegexFragmentAnalyzer : FragmentAnalyzer {
    override fun analyzeFragments(query: String): Set<FragmentAnalysis> {
        val fragmentReferences = FragmentRegexUtil.findFragmentReferences(query)
        val fragmentDefinitions = FragmentRegexUtil.findFragmentDefinitions(query)

        return fragmentReferences.map {
            FragmentAnalysis(fragmentReference = it, isDefined = fragmentDefinitions.contains(it))
        }.toSet()
    }
}