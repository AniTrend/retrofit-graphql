package co.anitrend.retrofit.graphql.data.user.koin

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.api.provider.extensions.api
import co.anitrend.retrofit.graphql.data.arch.database.extensions.db
import co.anitrend.retrofit.graphql.data.arch.extensions.onlineController
import co.anitrend.retrofit.graphql.data.user.converters.UserEntityConverter
import co.anitrend.retrofit.graphql.data.user.converters.UserModelConverter
import co.anitrend.retrofit.graphql.data.user.mapper.UserResponseMapper
import co.anitrend.retrofit.graphql.data.user.repository.UserRepositoryContract
import co.anitrend.retrofit.graphql.data.user.repository.UserRepositoryImpl
import co.anitrend.retrofit.graphql.data.user.source.UserSourceImpl
import co.anitrend.retrofit.graphql.data.user.source.contract.UserSource
import co.anitrend.retrofit.graphql.data.user.usecase.UserUseCaseContract
import co.anitrend.retrofit.graphql.data.user.usecase.UserUseCaseImpl
import org.koin.dsl.module

private val sourceModule = module {
    factory<UserSource> {
        UserSourceImpl(
            settings = get(),
            localSource = db().userDao(),
            remoteSource = api(EndpointType.GITHUB),
            strategy = onlineController(),
            mapper = get(),
            converter = get(),
            dispatchers = get()
        )
    }
}

private val converterModule = module {
    factory {
        UserModelConverter()
    }
    factory {
        UserEntityConverter()
    }
}

private val mapperModule = module {
    factory {
        UserResponseMapper(
            localSource = db().userDao(),
            converter = get()
        )
    }
}

private val useCaseModule = module {
    factory<UserUseCaseContract> {
        UserUseCaseImpl(
            repository = get()
        )
    }
}

private val repositoryModule = module {
    factory<UserRepositoryContract> {
        UserRepositoryImpl(
            source = get()
        )
    }
}

internal val userModules = listOf(
    sourceModule,
    mapperModule,
    useCaseModule,
    repositoryModule,
    converterModule
)