package co.anitrend.retrofit.graphql.data.bucket.usecase

import co.anitrend.arch.data.model.UserInterfaceState
import co.anitrend.arch.data.repository.contract.ISupportRepository
import co.anitrend.retrofit.graphql.data.bucket.repository.BucketRepositoryContract
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.domain.usecases.BucketUseCase

internal class BucketUseCaseImpl(
    repository: BucketRepositoryContract
) : BucketUseCaseContract(repository) {
    /**
     * Informs underlying repositories or related components running background operations to stop
     */
    override fun onCleared() {
        repository as ISupportRepository
        repository.onCleared()
    }
}

typealias BucketUseCaseContract = BucketUseCase<UserInterfaceState<List<BucketFile>>>