package co.anitrend.retrofit.graphql.data.market.converters

import co.anitrend.arch.data.converter.SupportConverter
import co.anitrend.retrofit.graphql.data.market.entity.MarketPlaceEntity
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
