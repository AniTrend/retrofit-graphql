package co.anitrend.retrofit.graphql.data.graphql.connection

import co.anitrend.retrofit.graphql.data.graphql.common.INode
import co.anitrend.retrofit.graphql.data.graphql.edge.IEdge
import co.anitrend.retrofit.graphql.data.graphql.paging.PagingInfo
import com.google.gson.annotations.SerializedName

internal interface IConnection {
    @get:SerializedName("edges") val edges: List<IEdge>?
    @get:SerializedName("nodes") val nodes: List<INode>?
    @get:SerializedName("pageInfo") val pageInfo: PagingInfo?
    @get:SerializedName("totalCount") val totalCount: Int
}