package co.anitrend.retrofit.graphql.data.api.interceptor

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.api.converter.request.SampleRequestConverter
import co.anitrend.retrofit.graphql.sample.BuildConfig
import io.github.wax911.library.annotation.processor.GraphProcessor
import io.github.wax911.library.converter.GraphConverter
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
        val requestBuilder = original.newBuilder()
            .header(ACCEPT, SampleRequestConverter.MIME_TYPE)
            .method(original.method, original.body)

        if (original.header(CONTENT_TYPE).isNullOrEmpty())
            requestBuilder.header(CONTENT_TYPE, GraphConverter.MimeType)

        if (original.url.host == EndpointType.GITHUB.url.host)
            requestBuilder.header("Authorization", "bearer ${BuildConfig.token}")

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        const val CONTENT_TYPE = "Content-Type"
        const val ACCEPT = "Accept"
    }
}