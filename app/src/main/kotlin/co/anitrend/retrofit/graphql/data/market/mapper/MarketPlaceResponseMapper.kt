package co.anitrend.retrofit.graphql.data.market.mapper

import co.anitrend.retrofit.graphql.data.arch.mapper.GraphQLMapper
import co.anitrend.retrofit.graphql.data.market.converters.MarketPlaceModelConverter
import co.anitrend.retrofit.graphql.data.market.datasource.local.MarketPlaceLocalSource
import co.anitrend.retrofit.graphql.data.market.entity.MarketPlaceEntity
import co.anitrend.retrofit.graphql.data.market.model.MarketPlaceListings

internal class MarketPlaceResponseMapper(
    private val localSource: MarketPlaceLocalSource,
    private val converter: MarketPlaceModelConverter
) : GraphQLMapper<MarketPlaceListings, List<MarketPlaceEntity>>() {
    /**
     * Inserts the given object into the implemented room database,
     *
     * @param mappedData mapped object from [onResponseMapFrom] to insert into the database
     */
    override suspend fun onResponseDatabaseInsert(mappedData: List<MarketPlaceEntity>) {
        if (mappedData.isNotEmpty())
            localSource.upsert(mappedData)
        else
            onEmptyResponse()
    }

    /**
     * Creates mapped objects and handles the database operations which may be required to map various objects,
     *
     * @param source the incoming data source type
     * @return mapped object that will be consumed by [onResponseDatabaseInsert]
     */
    override suspend fun onResponseMapFrom(source: MarketPlaceListings): List<MarketPlaceEntity> {
        val listings = source.marketplaceListings
        return converter.convertTo(listings.edges.orEmpty())
    }
}