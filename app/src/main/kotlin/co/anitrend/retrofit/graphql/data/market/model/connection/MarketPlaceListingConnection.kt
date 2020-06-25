package co.anitrend.retrofit.graphql.data.market.model.connection

import co.anitrend.retrofit.graphql.data.graphql.connection.IConnection
import co.anitrend.retrofit.graphql.data.graphql.paging.PagingInfo
import co.anitrend.retrofit.graphql.data.market.model.edge.MarketPlaceListingEdge
import co.anitrend.retrofit.graphql.data.market.model.node.MarketPlaceNode

internal data class MarketPlaceListingConnection(
    override val edges: List<MarketPlaceListingEdge>?,
    override val nodes: List<MarketPlaceNode>?,
    override val pageInfo: PagingInfo?,
    override val totalCount: Int
) : IConnection