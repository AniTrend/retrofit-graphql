package co.anitrend.retrofit.graphql.data.api.common

import co.anitrend.retrofit.graphql.sample.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal enum class EndpointType(val url: HttpUrl) {
    GITHUB(BuildConfig.github.toHttpUrl()),
    BUCKET(BuildConfig.bucket.toHttpUrl());

    companion object {
        const val BASE_ENDPOINT_PATH = "/graphql"
    }
}