/*
 *    Copyright 2020 AniTrend
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package co.anitrend.retrofit.graphql.data.arch.controller.policy

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingRequestHelper
import co.anitrend.arch.domain.entities.NetworkState
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import timber.log.Timber

/**
 * Does not run any connectivity check before prior to execution, this is useful for sources
 * that may have caching on the network level through interception or cache-control from
 * the origin server.
 */
internal class OfflineControllerPolicy<D> private constructor() : ControllerStrategy<D>() {

    /**
     * Execute a paging task under an implementation strategy
     *
     * @param block what will be executed
     * @param pagingRequestHelper paging event emitter
     */
    override suspend fun invoke(
        block: suspend () -> Unit,
        pagingRequestHelper: PagingRequestHelper.Request.Callback
    ) {
        runCatching {
            block()
            pagingRequestHelper.recordSuccess()
        }.exceptionOrNull()?.also { e ->
            Timber.tag(moduleTag).e(e)
            pagingRequestHelper.recordFailure(e)
        }
    }

    /**
     * Execute a task under an implementation strategy
     *
     * @param block what will be executed
     * @param networkState network state event emitter
     */
    override suspend fun invoke(
        block: suspend () -> D?,
        networkState: MutableLiveData<NetworkState>
    ): D? {
        return runCatching{
            networkState.postValue(NetworkState.Loading)
            val result = block()
            networkState.postValue(NetworkState.Success)
            result
        }.getOrElse {
            Timber.tag(moduleTag).e(it)
            networkState.postValue(
                NetworkState.Error(
                    heading = it.cause?.message ?: "Unexpected error encountered",
                    message = it.message
                )
            )
            null
        }
    }

    companion object {
        fun <T> create() =
            OfflineControllerPolicy<T>()
    }
}