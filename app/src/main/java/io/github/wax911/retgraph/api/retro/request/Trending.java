package io.github.wax911.retgraph.api.retro.request;

import java.util.List;

import io.github.wax911.library.annotation.GraphQuery;
import io.github.wax911.library.model.request.QueryContainerBuilder;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface Trending {

    @POST("/")
    @GraphQuery("Trending")
    @Headers("Content-Type: application/json")
    Call<List<Object>> getTrending(@Body QueryContainerBuilder request);
}
