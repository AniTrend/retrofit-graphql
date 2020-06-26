package co.anitrend.retrofit.graphql.data.market.converters

import co.anitrend.arch.data.converter.SupportConverter
import co.anitrend.arch.data.mapper.contract.ISupportMapperHelper
import co.anitrend.retrofit.graphql.data.arch.common.SampleMapper
import co.anitrend.retrofit.graphql.data.market.entity.MarketPlaceEntity
import co.anitrend.retrofit.graphql.data.market.model.edge.MarketPlaceListingEdge
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing

internal class MarketPlaceEntityConverter(
    override val fromType: (MarketPlaceEntity) -> MarketPlaceListing = { from().transform(it) },
    override val toType: (MarketPlaceListing) -> MarketPlaceEntity = { to().transform(it) }
) : SupportConverter<MarketPlaceEntity, MarketPlaceListing>() {
    companion object : SampleMapper<MarketPlaceEntity, MarketPlaceListing>() {
        override fun from() =
            object : ISupportMapperHelper<MarketPlaceEntity, MarketPlaceListing> {
                /**
                 * Transforms the the [source] to the target type
                 */
                override fun transform(source: MarketPlaceEntity) =
                    MarketPlaceListing(
                        id = source.id,
                        cursorId = source.cursorId,
                        logoUrl = source.logoUrl,
                        background = source.logoBackground,
                        name = source.name,
                        categories = source.categories,
                        slug = source.slug,
                        description = source.description,
                        isPaid = source.isPaid,
                        isVerified = source.isVerified
                    )
            }

        override fun to() =
            object : ISupportMapperHelper<MarketPlaceListing, MarketPlaceEntity> {
                /**
                 * Transforms the the [source] to the target type
                 */
                override fun transform(source: MarketPlaceListing): MarketPlaceEntity {
                    throw Throwable("Not yet implemented")
                }
            }
    }
}

internal class MarketPlaceModelConverter(
    override val fromType: (MarketPlaceEntity) -> MarketPlaceListingEdge = { from().transform(it) },
    override val toType: (MarketPlaceListingEdge) -> MarketPlaceEntity = { to().transform(it) }
) : SupportConverter<MarketPlaceEntity, MarketPlaceListingEdge>() {
    companion object : SampleMapper<MarketPlaceEntity, MarketPlaceListingEdge>() {
        override fun from() =
            object : ISupportMapperHelper<MarketPlaceEntity, MarketPlaceListingEdge> {
                /**
                 * Transforms the the [source] to the target type
                 */
                override fun transform(source: MarketPlaceEntity): MarketPlaceListingEdge {
                    throw Throwable("Not yet implemented")
                }
            }

        override fun to() =
            object : ISupportMapperHelper<MarketPlaceListingEdge, MarketPlaceEntity> {
                /**
                 * Transforms the the [source] to the target type
                 */
                override fun transform(source: MarketPlaceListingEdge): MarketPlaceEntity {
                    val categories = listOf(
                        source.node.primaryCategory.name,
                        source.node.secondaryCategory?.name
                    ).mapNotNull { it }

                    return MarketPlaceEntity(
                        id = source.node.id,
                        cursorId = source.cursor,
                        logoUrl = source.node.logo,
                        logoBackground = source.node.background,
                        name = source.node.name,
                        categories = categories,
                        slug = source.node.slug,
                        description = source.node.description,
                        isPaid = source.node.isPaid,
                        isVerified = source.node.isVerified
                    )
                }
            }
    }
}

