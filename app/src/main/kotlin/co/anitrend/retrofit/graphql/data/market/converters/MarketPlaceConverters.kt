package co.anitrend.retrofit.graphql.data.market.converters

import co.anitrend.arch.data.converter.SupportConverter
import co.anitrend.retrofit.graphql.data.market.entity.MarketPlaceEntity
import co.anitrend.retrofit.graphql.data.market.model.edge.MarketPlaceListingEdge
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing

internal class MarketPlaceEntityConverter(
    override val fromType: (MarketPlaceEntity) -> MarketPlaceListing = {
        MarketPlaceListing(
            id = it.id,
            cursorId = it.cursorId,
            logoUrl = it.logoUrl,
            background = it.logoBackground,
            name = it.name,
            categories = it.categories,
            slug = it.slug,
            description = it.description,
            isPaid = it.isPaid,
            isVerified = it.isVerified
        )
    },
    override val toType: (MarketPlaceListing) -> MarketPlaceEntity = { throw NotImplementedError() }
) : SupportConverter<MarketPlaceEntity, MarketPlaceListing>()

internal class MarketPlaceModelConverter(
    override val fromType: (MarketPlaceEntity) -> MarketPlaceListingEdge = { throw NotImplementedError() },
    override val toType: (MarketPlaceListingEdge) -> MarketPlaceEntity = {
        val categories = listOf(
            it.node.primaryCategory.name,
            it.node.secondaryCategory?.name
        ).mapNotNull { name -> name }

        MarketPlaceEntity(
            id = it.node.id,
            cursorId = it.cursor,
            logoUrl = it.node.logo,
            logoBackground = it.node.background,
            name = it.node.name,
            categories = categories,
            slug = it.node.slug,
            description = it.node.description,
            isPaid = it.node.isPaid,
            isVerified = it.node.isVerified
        )
    }
) : SupportConverter<MarketPlaceEntity, MarketPlaceListingEdge>()

