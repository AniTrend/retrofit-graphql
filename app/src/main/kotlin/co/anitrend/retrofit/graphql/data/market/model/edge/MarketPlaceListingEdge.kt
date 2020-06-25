package co.anitrend.retrofit.graphql.data.market.model.edge

import co.anitrend.retrofit.graphql.data.graphql.edge.IEdge
import co.anitrend.retrofit.graphql.data.market.model.node.MarketPlaceNode

internal data class MarketPlaceListingEdge(
    override val cursor: String,
    override val node: MarketPlaceNode
) : IEdge