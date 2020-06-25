package co.anitrend.retrofit.graphql.domain.entities.user

import co.anitrend.retrofit.graphql.domain.common.EntityId

data class User(
    override val id: String,
    val avatar: String,
    val bio: String,
    val status: Status,
    val username: String
) : EntityId {
    data class Status(
        val emoji: String,
        val message: String
    )
}