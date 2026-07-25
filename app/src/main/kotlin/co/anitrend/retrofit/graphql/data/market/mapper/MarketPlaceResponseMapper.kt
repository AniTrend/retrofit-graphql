package co.anitrend.retrofit.graphql.data.market.mapper

import co.anitrend.retrofit.graphql.data.arch.mapper.GraphQLMapper
import co.anitrend.retrofit.graphql.data.market.datasource.local.MarketPlaceLocalSource
import co.anitrend.retrofit.graphql.data.market.entity.MarketPlaceEntity
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData

internal class MarketPlaceResponseMapper(
    private val localSource: MarketPlaceLocalSource,
) : GraphQLMapper<GetMarketPlaceAppsData, List<MarketPlaceEntity>>() {
    /**
     * Inserts the given object into the implemented room database.
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
     * Creates mapped objects and handles the database operations which may be required to map various objects.
     *
     * @param source the incoming data source type
     * @return mapped object that will be consumed by [onResponseDatabaseInsert]
     */
    override suspend fun onResponseMapFrom(source: GetMarketPlaceAppsData): List<MarketPlaceEntity> {
        val listings = source.marketplaceListings
        return listings.edges.orEmpty().mapNotNull { edge ->
            edge ?: return@mapNotNull null
            val node = edge.node ?: return@mapNotNull null
            val categories = listOfNotNull(
                node.primaryCategory.name,
                node.secondaryCategory?.name,
            )

            MarketPlaceEntity(
                id = node.id,
                cursorId = edge.cursor,
                logoUrl = node.logoUrl,
                logoBackground = node.logoBackgroundColor,
                name = node.name,
                categories = categories,
                slug = node.slug,
                description = node.shortDescription,
                isPaid = node.isPaid,
                isVerified = node.isVerified,
            )
        }
    }
}
