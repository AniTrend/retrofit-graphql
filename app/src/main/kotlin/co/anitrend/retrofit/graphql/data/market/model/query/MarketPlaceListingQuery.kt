package co.anitrend.retrofit.graphql.data.market.model.query

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
)
