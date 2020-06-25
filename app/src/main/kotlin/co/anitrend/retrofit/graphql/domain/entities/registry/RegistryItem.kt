package co.anitrend.retrofit.graphql.domain.entities.registry

import co.anitrend.retrofit.graphql.domain.common.EntityId

/**
 * Represents a github repository
 */
data class RegistryItem(
    override val id: String,
    val description: String,
    val forkCount: Int,
    val isFork: Boolean,
    val isLocked: Boolean,
    val isPrivate: Boolean,
    val isTemplate: Boolean,
    val name: String,
    val projectsUrl: String,
    val url: String,
    val viewerHasStarred: Int,
    val watchersCount: Int
) : EntityId