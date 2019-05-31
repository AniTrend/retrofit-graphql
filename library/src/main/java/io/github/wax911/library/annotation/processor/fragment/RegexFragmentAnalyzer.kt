package io.github.wax911.library.annotation.processor.fragment

/**
 * An implementation of FragmentAnalyzer that simply uses regular expressions to find fragment references, and whether
 * they are defined with the query.
 */
class RegexFragmentAnalyzer : FragmentAnalyzer {
    override fun analyzeFragments(graphqlContent: String): Set<FragmentAnalysis> {
        val fragmentReferences = GraphRegexUtil.findFragmentReferences(graphqlContent)
        val fragmentDefinitions = GraphRegexUtil.findFragmentDefinitions(graphqlContent)

        return fragmentReferences.map {
            FragmentAnalysis(fragmentReference = it, isDefined = fragmentDefinitions.contains(it))
        }.toSet()
    }
}