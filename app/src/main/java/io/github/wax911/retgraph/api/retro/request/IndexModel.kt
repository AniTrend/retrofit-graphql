package io.github.wax911.retgraph.api.retro.request

import io.github.wax911.library.annotation.GraphQuery
import io.github.wax911.library.model.body.GraphContainer
import io.github.wax911.library.model.request.QueryContainerBuilder
import io.github.wax911.retgraph.model.container.EntryFeed
import io.github.wax911.retgraph.model.container.TrendingFeed
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface IndexModel {

    @POST("graphql")
    @GraphQuery("Trending")
    @Headers("Content-Type: application/json")
    suspend fun getTrending(@Body request: QueryContainerBuilder): Response<GraphContainer<TrendingFeed>>

    @POST("graphql")
    @GraphQuery("RepoEntries")
    @Headers("Content-Type: application/json")
    suspend fun getRepoEntries(@Body request: QueryContainerBuilder): Response<GraphContainer<EntryFeed>>
}
