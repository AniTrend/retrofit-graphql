package co.anitrend.retrofit.graphql.data.user.model.node

import co.anitrend.retrofit.graphql.data.graphql.common.INode
import com.google.gson.annotations.SerializedName

internal data class UserNode(
    @SerializedName("id") override val id: String,
    @SerializedName("avatarUrl") val avatarUrl: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("status") val status: Status?,
    @SerializedName("login") val login: String
) : INode {
    data class Status(
        @SerializedName("emoji") val emoji: String?,
        @SerializedName("message") val message: String?
    )
}