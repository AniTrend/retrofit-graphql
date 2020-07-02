package co.anitrend.retrofit.graphql.data.bucket.usecase.upload

import co.anitrend.arch.data.model.UserInterfaceState
import co.anitrend.arch.data.repository.contract.ISupportRepository
import co.anitrend.retrofit.graphql.data.bucket.repository.upload.UploadRepositoryContract
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.domain.usecases.UploadUseCase

internal class UploadUseCaseImpl(
    repository: UploadRepositoryContract
) : UploadUseCaseContract(repository) {
    /**
     * Informs underlying repositories or related components running background operations to stop
     */
    override fun onCleared() {
        repository as ISupportRepository
        repository.onCleared()
    }
}

typealias UploadUseCaseContract = UploadUseCase<UserInterfaceState<BucketFile>>