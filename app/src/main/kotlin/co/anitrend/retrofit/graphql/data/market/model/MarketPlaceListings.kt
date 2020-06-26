package co.anitrend.retrofit.graphql.data.market.model

import co.anitrend.retrofit.graphql.data.market.model.connection.MarketPlaceListingConnection
import com.google.gson.annotations.SerializedName

internal data class MarketPlaceListings(
    @SerializedName("marketplaceListings")
    val marketplaceListings: MarketPlaceListingConnection
)