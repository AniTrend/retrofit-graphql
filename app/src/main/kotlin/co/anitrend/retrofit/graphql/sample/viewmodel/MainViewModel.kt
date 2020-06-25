package co.anitrend.retrofit.graphql.sample.viewmodel

import androidx.lifecycle.ViewModel
import co.anitrend.retrofit.graphql.data.user.usecase.UserUseCaseContract
import co.anitrend.retrofit.graphql.sample.viewmodel.model.MainState

class MainViewModel(
    useCase: UserUseCaseContract
) : ViewModel() {
    val state = MainState(useCase)

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     *
     * It is useful when ViewModel observes some data and you need to clear this subscription to
     * prevent a leak of this ViewModel.
     */
    override fun onCleared() {
        state.onCleared()
        super.onCleared()
    }
}