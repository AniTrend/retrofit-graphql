package co.anitrend.retrofit.graphql.data.bucket.repository.upload

import co.anitrend.arch.data.repository.SupportRepository
import co.anitrend.arch.data.state.DataState
import co.anitrend.arch.data.state.DataState.Companion.create
import co.anitrend.retrofit.graphql.data.bucket.source.upload.contract.BucketUploadSource
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.domain.models.common.IGraphQuery
import co.anitrend.retrofit.graphql.domain.repositories.UploadRepository

internal class UploadRepositoryImpl(
    private val source: BucketUploadSource
) : SupportRepository(source), UploadRepositoryContract {

    override fun uploadToBucket(mutation: IGraphQuery) =
        source.create(
            model = source(mutation)
        )
}

typealias UploadRepositoryContract = UploadRepository<DataState<BucketFile>>