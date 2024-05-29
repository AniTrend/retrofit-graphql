package co.anitrend.retrofit.graphql.data.market.koin

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.api.provider.extensions.api
import co.anitrend.retrofit.graphql.data.arch.database.extensions.db
import co.anitrend.retrofit.graphql.data.arch.extensions.onlineController
import co.anitrend.retrofit.graphql.data.market.converters.MarketPlaceEntityConverter
import co.anitrend.retrofit.graphql.data.market.converters.MarketPlaceModelConverter
import co.anitrend.retrofit.graphql.data.market.mapper.MarketPlaceResponseMapper
import co.anitrend.retrofit.graphql.data.market.repository.MarketPlaceRepositoryContract
import co.anitrend.retrofit.graphql.data.market.repository.MarketPlaceRepositoryImpl
import co.anitrend.retrofit.graphql.data.market.source.MarketPlaceSourceImpl
import co.anitrend.retrofit.graphql.data.market.source.contract.MarketPlaceSource
import co.anitrend.retrofit.graphql.data.market.usecase.MarketPlaceUseCaseContract
import co.anitrend.retrofit.graphql.data.market.usecase.MarketPlaceUseCaseImpl
import org.koin.dsl.module

private val sourceModule = module {
    factory<MarketPlaceSource> {
        MarketPlaceSourceImpl(
            localSource = db().appStoreDao(),
            remoteSource = api(EndpointType.GITHUB),
            strategy = onlineController(),
            mapper = get(),
            converter = get(),
            dispatcher = get()
        )
    }
}

private val converterModule = module {
    factory {
        MarketPlaceModelConverter()
    }
    factory {
        MarketPlaceEntityConverter()
    }
}

private val mapperModule = module {
    factory {
        MarketPlaceResponseMapper(
            localSource = db().appStoreDao(),
            converter = get()
        )
    }
}

private val useCaseModule = module {
    factory<MarketPlaceUseCaseContract> {
        MarketPlaceUseCaseImpl(
            repository = get()
        )
    }
}

private val repositoryModule = module {
    factory<MarketPlaceRepositoryContract> {
        MarketPlaceRepositoryImpl(
            source = get()
        )
    }
}

internal val marketPlaceModules = listOf(
    sourceModule,
    mapperModule,
    useCaseModule,
    repositoryModule,
    converterModule
)
