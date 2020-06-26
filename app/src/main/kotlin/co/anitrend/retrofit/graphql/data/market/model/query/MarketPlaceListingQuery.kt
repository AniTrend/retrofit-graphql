package co.anitrend.retrofit.graphql.data.market.model.query

import co.anitrend.retrofit.graphql.domain.models.common.IGraphQuery

/**
 * Look up Marketplace listings
 *
 * @param after The elements in the list that come after the specified cursor
 * @param before The elements in the list that come before the specified cursor
 * @param first The first _n_ elements from the list
 */
data class MarketPlaceListingQuery(
    var after: String? = null,
    var before: String? = null,
    val first: Int
) : IGraphQuery {
    override fun toMap() =
        mapOf(
            "after" to after,
            "before" to before,
            "first" to first
        )
}