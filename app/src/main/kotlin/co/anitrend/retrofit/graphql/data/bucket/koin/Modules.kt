package co.anitrend.retrofit.graphql.data.bucket.koin

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.api.provider.extensions.api
import co.anitrend.retrofit.graphql.data.arch.extensions.onlineController
import co.anitrend.retrofit.graphql.data.bucket.mapper.BucketResponseMapper
import co.anitrend.retrofit.graphql.data.bucket.mapper.UploadResponseMapper
import co.anitrend.retrofit.graphql.data.bucket.repository.BucketRepositoryContract
import co.anitrend.retrofit.graphql.data.bucket.repository.BucketRepositoryImpl
import co.anitrend.retrofit.graphql.data.bucket.repository.upload.UploadRepositoryContract
import co.anitrend.retrofit.graphql.data.bucket.repository.upload.UploadRepositoryImpl
import co.anitrend.retrofit.graphql.data.bucket.source.browse.BucketSourceImpl
import co.anitrend.retrofit.graphql.data.bucket.source.browse.contract.BucketSource
import co.anitrend.retrofit.graphql.data.bucket.source.upload.BucketUploadSourceImpl
import co.anitrend.retrofit.graphql.data.bucket.source.upload.contract.BucketUploadSource
import co.anitrend.retrofit.graphql.data.bucket.usecase.BucketUseCaseContract
import co.anitrend.retrofit.graphql.data.bucket.usecase.BucketUseCaseImpl
import co.anitrend.retrofit.graphql.data.bucket.usecase.upload.UploadUseCaseContract
import co.anitrend.retrofit.graphql.data.bucket.usecase.upload.UploadUseCaseImpl
import org.koin.dsl.module

private val sourceModule = module {
    factory<BucketSource> {
        BucketSourceImpl(
            remoteSource = api(EndpointType.BUCKET),
            strategy = onlineController(),
            dispatchers = get(),
            mapper = get()
        )
    }
    factory<BucketUploadSource> {
        BucketUploadSourceImpl(
            remoteSource = api(EndpointType.BUCKET),
            strategy = onlineController(),
            dispatchers = get(),
            mapper = get()
        )
    }
}

private val mapperModule = module {
    factory {
        BucketResponseMapper()
    }
    factory {
        UploadResponseMapper()
    }
}

private val useCaseModule = module {
    factory<BucketUseCaseContract> {
        BucketUseCaseImpl(
            repository = get()
        )
    }
    factory<UploadUseCaseContract> {
        UploadUseCaseImpl(
            repository = get()
        )
    }
}

private val repositoryModule = module {
    factory<BucketRepositoryContract> {
        BucketRepositoryImpl(
            source = get()
        )
    }
    factory<UploadRepositoryContract> {
        UploadRepositoryImpl(
            source = get()
        )
    }
}

internal val bucketModules = listOf(
    sourceModule,
    mapperModule,
    useCaseModule,
    repositoryModule
)