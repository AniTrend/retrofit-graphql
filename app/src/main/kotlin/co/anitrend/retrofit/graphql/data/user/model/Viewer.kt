package co.anitrend.retrofit.graphql.data.user.model

import co.anitrend.retrofit.graphql.data.user.model.node.UserNode
import com.google.gson.annotations.SerializedName

internal data class Viewer(
    @SerializedName("viewer") val viewer: UserNode
)