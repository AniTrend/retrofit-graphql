package io.github.wax911.library.annotation.processor.fragment

object FragmentRegexUtil {
    // Allowed GraphQL names characters are documented here: https://graphql.github.io/graphql-spec/draft/#sec-Names
    // We want to find all occurrences of "...SomeFragment". And the only piece we care about extracting is the
    // name of the fragment ("SomeFragment", group(2) in the regex match result).
    private const val REGEX_FRAGMENT_REFERENCE = "\\.\\.\\.(\\s+)?([_A-Za-z][_0-9A-Za-z]*)"
    private const val GROUP_FRAGMENT_REFERENCE = 2

    fun findFragmentReferences(query: String): Set<String> {
        val fragmentRefMatches = REGEX_FRAGMENT_REFERENCE
            .toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
            .findAll(query)

        return fragmentRefMatches.filter {
            it.groupValues.size >= (GROUP_FRAGMENT_REFERENCE + 1)
        }.map {
            it.groupValues[GROUP_FRAGMENT_REFERENCE]
        }.toSet()
    }
}