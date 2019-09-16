package io.github.wax911.retgraph.model.parent

import io.github.wax911.retgraph.model.Repository
import io.github.wax911.retgraph.model.User
import io.github.wax911.retgraph.model.Vote

data class Entry(
        val id: Long,
        val vote: Vote?,
        val score: Double,
        val postedBy: User?,
        val hotScore: Double,
        val repository: Repository?
)
