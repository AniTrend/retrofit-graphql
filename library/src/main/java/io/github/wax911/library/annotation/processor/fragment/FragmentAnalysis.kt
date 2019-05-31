package io.github.wax911.library.annotation.processor.fragment

/**
 * A simple data class that defines a fragment reference (by name), and whether or not it was defined with some graphql
 * content.
 */
data class FragmentAnalysis(val fragmentReference: String, val isDefined: Boolean)