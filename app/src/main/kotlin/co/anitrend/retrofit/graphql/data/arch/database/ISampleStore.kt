package co.anitrend.retrofit.graphql.data.arch.database

import co.anitrend.retrofit.graphql.data.market.datasource.local.MarketPlaceLocalSource
import co.anitrend.retrofit.graphql.data.user.datasource.local.UserLocalSource

internal interface ISampleStore {
    fun appStoreDao(): MarketPlaceLocalSource
    fun userDao(): UserLocalSource
}