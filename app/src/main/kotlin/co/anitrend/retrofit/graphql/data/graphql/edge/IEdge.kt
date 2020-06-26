package co.anitrend.retrofit.graphql.data.graphql.edge

import co.anitrend.retrofit.graphql.data.graphql.common.INode
import com.google.gson.annotations.SerializedName

internal interface IEdge {
    @get:SerializedName("cursor")
    val cursor: String
    @get:SerializedName("node")
    val node: INode?
}