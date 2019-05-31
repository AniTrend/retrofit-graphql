package io.github.wax911.library.annotation.processor.fragment

/**
 * This util class provides helpful methods for finding fragment information in a GraphQL query. It has a method for
 * finding all references to fragments within a query. And another method for finding all defined fragments, which may
 * exist after the query.
 */
object FragmentRegexUtil {
    // Allowed GraphQL names characters are documented here: https://graphql.github.io/graphql-spec/draft/#sec-Names
    // We want to find all occurrences of "...SomeFragment". And the only piece we care about extracting is the
    // name of the fragment ("SomeFragment")
    private const val REGEX_FRAGMENT_NAME = "[_A-Za-z][_0-9A-Za-z]*"
    private const val REGEX_FRAGMENT_REFERENCE = "\\.\\.\\.(\\s+)?($REGEX_FRAGMENT_NAME)"
    private const val REGEX_FRAGMENT_DEFINITION = "fragment\\s{1,}([_A-Za-z][_0-9A-Za-z]*)\\s{1,}on"
    // The fragment name will be found in group 2 of the reference regex match result.
    private const val GROUP_FRAGMENT_REFERENCE = 2
    // The fragment name will be found in group 1 of the definition regex match result.
    private const val GROUP_FRAGMENT_DEFINITION = 1

    /**
     * Finds all distinct references to fragments in a query. A set of all unique fragment names (which are the
     * references) is returned.
     */
    fun findFragmentReferences(query: String): Set<String> {
        return extractFragmentNames(query, REGEX_FRAGMENT_REFERENCE, GROUP_FRAGMENT_REFERENCE)
    }

    /**
     * Finds all defined fragments, which are usually found after a query. A set of all unique fragment names is
     * returned.
     */
    fun findFragmentDefinitions(query: String): Set<String> {
        return extractFragmentNames(query, REGEX_FRAGMENT_DEFINITION, GROUP_FRAGMENT_DEFINITION)
    }

    /**
     * Finds all strings defined by "regexStr" in the provided "query". The regex might return multiple match groups,
     * so the "groupIndex" indicating the position of the expecting string should be specified.
     */
    private fun extractFragmentNames(query: String, regexStr: String, groupIndex: Int): Set<String> {
        val regexMatches = regexStr
            .toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
            .findAll(query)

        return regexMatches.filter {
            it.groupValues.size >= (groupIndex + 1)
        }.map {
            it.groupValues[groupIndex]
        }.toSet()
    }
}