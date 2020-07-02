package co.anitrend.retrofit.graphql.sample.view.content.bucket.viewmodel

import androidx.lifecycle.ViewModel
import co.anitrend.retrofit.graphql.data.bucket.usecase.BucketUseCaseContract
import co.anitrend.retrofit.graphql.data.bucket.usecase.upload.UploadUseCaseContract
import co.anitrend.retrofit.graphql.sample.view.content.bucket.viewmodel.state.BucketState
import co.anitrend.retrofit.graphql.sample.view.content.bucket.viewmodel.state.UploadState

class BucketViewModel(
    bucketUseCase: BucketUseCaseContract,
    uploadUseCase: UploadUseCaseContract
) : ViewModel() {

    val bucketState = BucketState(bucketUseCase)
    val uploadState = UploadState(uploadUseCase)

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     *
     * It is useful when ViewModel observes some data and you need to clear this subscription to
     * prevent a leak of this ViewModel.
     */
    override fun onCleared() {
        bucketState.onCleared()
        uploadState.onCleared()
        super.onCleared()
    }
}