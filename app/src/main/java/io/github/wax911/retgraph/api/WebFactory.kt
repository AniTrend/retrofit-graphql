package io.github.wax911.retgraph.api

import retrofit2.Retrofit

/**
 * Created by max on 2018/04/05.
 * Retrofit service factory
 */
class WebFactory(private val retrofit: Retrofit) {

    /**
     * Generates retrofit service classes
     *
     * @param service Interface class method representing your request to use
     */
    fun <S> create(service: Class<S>): S = retrofit.create(service)
}
