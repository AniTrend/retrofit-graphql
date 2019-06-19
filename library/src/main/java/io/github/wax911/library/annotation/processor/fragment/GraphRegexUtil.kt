package io.github.wax911.library.annotation.processor.fragment

/**
 * This util class provides helpful methods for finding fragment and query information in a GraphQL String. It has a
 * method for finding all references to fragments. Another method for finding all defined fragments, which may exist
 * after a query. And a method to test whether the current GraphQL String contains a query.
 */
object GraphRegexUtil {
    // Allowed GraphQL names characters are documented here: https://graphql.github.io/graphql-spec/draft/#sec-Names
    // We want to find all occurrences of "...SomeFragment". And the only piece we care about extracting is the
    // name of the fragment ("SomeFragment")
    private const val REGEX_VALID_NAME = "[_A-Za-z][_0-9A-Za-z]*"
    private const val REGEX_FRAGMENT_REFERENCE = "\\.\\.\\.(\\s+)?($REGEX_VALID_NAME)"
    private const val REGEX_FRAGMENT_DEFINITION = "fragment\\s{1,}($REGEX_VALID_NAME)\\s{1,}on"
    private const val REGEX_OPERATION_DEFINITION = "(query|mutation|subscription)\\s+($REGEX_VALID_NAME)"
    // The fragment name will be found in group 2 of the reference regex match result.
    private const val GROUP_FRAGMENT_REFERENCE = 2
    // The fragment name will be found in group 1 of the definition regex match result.
    private const val GROUP_FRAGMENT_DEFINITION = 1

    private val regexOptions = setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)

    /**
     * Finds all distinct references to fragments in some GraphQL String. A set of all unique fragment names
     * (which are the references) is returned.
     */
    fun findFragmentReferences(graphqlContent: String): Set<String> {
        return extractFragmentNames(graphqlContent, REGEX_FRAGMENT_REFERENCE, GROUP_FRAGMENT_REFERENCE)
    }

    /**
     * Finds all defined fragments, which are usually found after a query. A set of all unique fragment names is
     * returned.
     */
    fun findFragmentDefinitions(graphqlContent: String): Set<String> {
        return extractFragmentNames(graphqlContent, REGEX_FRAGMENT_DEFINITION, GROUP_FRAGMENT_DEFINITION)
    }

    /**
     * Returns whether or not a valid GraphQL operation (query, mutation, or subscription) can be found in some GraphQL
     * String.
     */
    fun containsAnOperation(graphqlContent: String): Boolean {
        return (REGEX_OPERATION_DEFINITION.toRegex(regexOptions).containsMatchIn(graphqlContent))
    }

    /**
     * Finds all strings defined by "regexStr" in the provided "graphqlContent". The regex will return multiple match
     * groups, so the "groupIndex" indicating the position of the expecting string should be specified.
     */
    private fun extractFragmentNames(graphqlContent: String, regexStr: String, groupIndex: Int): Set<String> {
        val regexMatches = regexStr
            .toRegex(regexOptions)
            .findAll(graphqlContent)

        return regexMatches.filter {
            it.groupValues.size >= (groupIndex + 1)
        }.map {
            it.groupValues[groupIndex]
        }.toSet()
    }
}