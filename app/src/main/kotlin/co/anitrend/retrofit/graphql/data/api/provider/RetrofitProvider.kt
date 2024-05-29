package co.anitrend.retrofit.graphql.data.api.provider

import androidx.collection.LruCache
import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.api.interceptor.GraphClientInterceptor
import co.anitrend.retrofit.graphql.sample.BuildConfig
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import retrofit2.Retrofit
import timber.log.Timber


/**
 * Factory to supply types retrofit instances
 */
internal object RetrofitProvider {

    private val retrofitCache = LruCache<EndpointType, Retrofit>(3)

    private fun provideOkHttpClient(endpointType: EndpointType, scope: Scope) : OkHttpClient {
        val builder = scope.get<OkHttpClient.Builder> {
            parametersOf(
                when (endpointType) {
                    EndpointType.BUCKET -> HttpLoggingInterceptor.Level.BODY
                    else -> HttpLoggingInterceptor.Level.HEADERS
                }
            )
        }

        when (endpointType) {
            EndpointType.BUCKET,
            EndpointType.GITHUB -> {
                Timber.d("""
                    Adding request interceptors for endpoint: ${endpointType.name}
                    """.trimIndent()
                )
                builder.addInterceptor(GraphClientInterceptor())
                if (BuildConfig.DEBUG)
                    builder.addInterceptor(
                        scope.get<ChuckerInterceptor> {
                            parametersOf(setOf("Authorization"))
                        }
                    )
            }
        }

        return builder.build()
    }

    private fun createRetrofit(endpointType: EndpointType, scope: Scope) : Retrofit {
        return scope.get<Retrofit.Builder>()
            .client(
                provideOkHttpClient(
                    endpointType,
                    scope
                )
            )
            .baseUrl(endpointType.url)
            .build()
    }

    fun provideRetrofit(endpointType: EndpointType, scope: Scope): Retrofit {
        val reference = retrofitCache.get(endpointType)
        return if (reference != null) {
            Timber.d(
                "Using cached retrofit instance for endpoint: ${endpointType.name}"
            )
            reference
        }
        else {
            Timber.d(
                "Creating new retrofit instance for endpoint: ${endpointType.name}"
            )
            val retrofit =
                createRetrofit(
                    endpointType,
                    scope
                )
            retrofitCache.put(endpointType, retrofit)
            retrofit
        }
    }
}