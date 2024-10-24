package co.anitrend.retrofit.graphql.data.bucket.repository

import co.anitrend.arch.data.repository.SupportRepository
import co.anitrend.arch.data.state.DataState
import co.anitrend.arch.data.state.DataState.Companion.create
import co.anitrend.retrofit.graphql.data.bucket.source.browse.contract.BucketSource
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.domain.repositories.BucketRepository

internal class BucketRepositoryImpl(
    private val source: BucketSource
) : SupportRepository(source), BucketRepositoryContract {
    override fun getAllFiles() =
        source.create(
            model = source()
        )
}

typealias BucketRepositoryContract = BucketRepository<DataState<List<BucketFile>>>