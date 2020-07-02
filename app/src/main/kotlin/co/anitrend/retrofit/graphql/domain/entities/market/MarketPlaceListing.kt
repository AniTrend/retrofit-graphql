package co.anitrend.retrofit.graphql.domain.entities.market

import co.anitrend.retrofit.graphql.domain.common.EntityId

/**
 * Represents a market place item
 */
data class MarketPlaceListing(
    override val id: String,
    val cursorId: String,
    val logoUrl: String?,
    val background: String,
    val name: String,
    val categories: List<String>,
    val slug: String,
    val description: String,
    val isPaid: Boolean,
    val isVerified: Boolean
) : EntityId