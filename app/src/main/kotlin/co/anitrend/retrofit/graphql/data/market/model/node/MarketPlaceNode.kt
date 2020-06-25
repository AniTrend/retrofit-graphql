package co.anitrend.retrofit.graphql.data.market.model.node

import co.anitrend.retrofit.graphql.data.graphql.common.INode
import com.google.gson.annotations.SerializedName

internal data class MarketPlaceNode(
    @SerializedName("id") override val id: String,
    @SerializedName("primaryCategory") val primaryCategory: Category,
    @SerializedName("secondaryCategory") val secondaryCategory: Category?,
    @SerializedName("slug") val slug: String,
    @SerializedName("shortDescription") val description: String,
    @SerializedName("isPaid") val isPaid: Boolean,
    @SerializedName("isVerified") val isVerified: Boolean,
    @SerializedName("logoBackgroundColor") val background: String,
    @SerializedName("logoUrl") val logo: String?,
    @SerializedName("name") val name: String
) : INode {
    data class Category(
        @SerializedName("name") val name: String
    )
}