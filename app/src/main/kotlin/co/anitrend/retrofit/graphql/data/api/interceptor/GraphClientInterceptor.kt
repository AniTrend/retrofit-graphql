package co.anitrend.retrofit.graphql.data.api.interceptor

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.sample.BuildConfig
import co.anitrend.retrofit.graphql.converter.GraphConverter
import okhttp3.Interceptor
import okhttp3.Response

/**
 * ClientInterceptor interceptor add headers dynamically adds accept headers.
 * The context in which an [Interceptor] may be  parallel or asynchronous depending
 * on the dispatching caller, as such take care to assure thread safety
 */
internal class GraphClientInterceptor: Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val bodyContentType = original.body?.contentType()
        val requestBuilder = original.newBuilder()
            .header(ACCEPT, APPLICATION_JSON)
            .method(original.method, original.body)

        if (original.header(CONTENT_TYPE).isNullOrEmpty() && bodyContentType == null)
            requestBuilder.header(CONTENT_TYPE, GraphConverter.MIME_TYPE)

        if (original.url.host == EndpointType.GITHUB.url.host)
            requestBuilder.header("Authorization", "bearer ${BuildConfig.token}")

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        const val CONTENT_TYPE = "Content-Type"
        const val ACCEPT = "Accept"
        private const val APPLICATION_JSON = "application/json"
    }
}
