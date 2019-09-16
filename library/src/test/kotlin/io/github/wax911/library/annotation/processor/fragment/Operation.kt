package io.github.wax911.library.annotation.processor.fragment

data class Operation(val typeStr: String, val nameStr: String) {
    companion object {
        fun enumeration() = listOf(
            Operation("query", "Query"),
            Operation("mutation", "Mutation"),
            Operation("subscription", "Subscription")
        )
    }
}