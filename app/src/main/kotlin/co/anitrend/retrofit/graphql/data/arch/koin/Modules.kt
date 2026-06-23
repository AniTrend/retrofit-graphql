package co.anitrend.retrofit.graphql.data.arch.koin

import co.anitrend.retrofit.graphql.converter.GraphConverter
import co.anitrend.retrofit.graphql.data.api.converter.RequestBodyPassThroughConverterFactory
import co.anitrend.retrofit.graphql.data.arch.database.SampleStore
import co.anitrend.retrofit.graphql.data.bucket.koin.bucketModules
import co.anitrend.retrofit.graphql.data.market.koin.marketPlaceModules
import co.anitrend.retrofit.graphql.data.user.koin.userModules
import co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry
import co.anitrend.retrofit.graphql.logger.contract.ILogger
import co.anitrend.retrofit.graphql.sample.BuildConfig
import co.anitrend.retrofit.graphql.sample.generated.GeneratedGraphQLRegistry
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

private val coreModule = module {
    single {
        SampleStore.create(
            applicationContext = androidContext()
        )
    }
    single<GraphQLDocumentRegistry> {
        GeneratedGraphQLRegistry
    }
}

private val networkModule = module {
    factory {
        val level = if (BuildConfig.DEBUG)
            ILogger.Level.VERBOSE
        else
            ILogger.Level.ERROR

        Retrofit.Builder()
            .addConverterFactory(
                RequestBodyPassThroughConverterFactory
            )
            .addConverterFactory(
                GraphConverter.create(
                    context = androidContext(),
                    registry = get<GraphQLDocumentRegistry>(),
                    level = level,
                )
            )
    }
}

private val interceptorModules = module {
    factory { (exclusionHeaders: Set<String>) ->
        ChuckerInterceptor.Builder(
            context = androidContext()
            // The previously created Collector
        )
            .collector(
                collector = ChuckerCollector(
                    context = androidContext(),
                    // Toggles visibility of the push notification
                    showNotification = true,
                    // Allows to customize the retention period of collected data
                    retentionPeriod = RetentionManager.Period.ONE_HOUR
                )
                // The max body content length in bytes, after this responses will be truncated.
            )
            .maxContentLength(
                length = 10500L
                // List of headers to replace with ** in the Chucker UI
            )
            .redactHeaders(
                headerNames = exclusionHeaders
            )
            .alwaysReadResponseBody(false)
            .build()
    }
    factory { (interceptorLogLevel: HttpLoggingInterceptor.Level) ->
        val okHttpClientBuilder = OkHttpClient.Builder()
            .cache(
                Cache(
                    androidContext().externalCacheDir ?: androidContext().cacheDir,
                    (1024 * 1024 * 25).toLong()
                )
            )
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        when {
            BuildConfig.DEBUG -> {
                val httpLoggingInterceptor = HttpLoggingInterceptor()
                httpLoggingInterceptor.level = interceptorLogLevel
                okHttpClientBuilder.addInterceptor(httpLoggingInterceptor)
            }
        }

        okHttpClientBuilder
    }
}

val dataModules = listOf(
    coreModule,
    networkModule,
    interceptorModules
) + marketPlaceModules + userModules + bucketModules
