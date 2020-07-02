package co.anitrend.retrofit.graphql.data.graphql.paging

import com.google.gson.annotations.SerializedName

internal data class PagingInfo(
    @SerializedName("endCursor") val endCursor: String?,
    @SerializedName("hasNextPage") val hasNextPage: Boolean,
    @SerializedName("hasPreviousPage") val hasPreviousPage: Boolean,
    @SerializedName("startCursor") val startCursor: String?
)