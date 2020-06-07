package io.github.wax911.library.annotation.processor.fragment

/**
 * A contract for something that can parse graphql content and provide a full analysis of what fragments are referenced,
 * and whether the fragments are defined with the query.
 */
interface FragmentAnalyzer {
    fun analyzeFragments(graphqlContent: String): Set<FragmentAnalysis>
}