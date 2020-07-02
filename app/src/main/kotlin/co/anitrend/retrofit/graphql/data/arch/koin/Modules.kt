package co.anitrend.retrofit.graphql.data.arch.koin

import co.anitrend.retrofit.graphql.data.api.converter.SampleConverterFactory
import co.anitrend.retrofit.graphql.data.arch.database.SampleStore
import co.anitrend.retrofit.graphql.data.bucket.koin.bucketModules
import co.anitrend.retrofit.graphql.data.market.koin.marketPlaceModules
import co.anitrend.retrofit.graphql.data.user.koin.userModules
import co.anitrend.retrofit.graphql.sample.BuildConfig
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import io.github.wax911.library.annotation.processor.GraphProcessor
import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.annotation.processor.plugin.AssetManagerDiscoveryPlugin
import io.github.wax911.library.logger.DefaultGraphLogger
import io.github.wax911.library.logger.contract.ILogger
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
    factory {
        AssetManagerDiscoveryPlugin(
            assetManager = androidContext().assets
        )
    }
    single {
        val level = if (BuildConfig.DEBUG)
            ILogger.Level.VERBOSE
        else
            ILogger.Level.ERROR

        GraphProcessor(
            discoveryPlugin = get<AssetManagerDiscoveryPlugin>(),
            logger = DefaultGraphLogger(level)
        )
    }
}

private val networkModule = module {
    factory {
        val converterFactory = SampleConverterFactory(
            processor = get<GraphProcessor>()
        )
        Retrofit.Builder()
            .addConverterFactory(
                converterFactory
            )
    }
}

private val interceptorModules = module {
    factory { (exclusionHeaders: Set<String>) ->
        ChuckerInterceptor(
            context = androidContext(),
            // The previously created Collector
            collector = ChuckerCollector(
                context = androidContext(),
                // Toggles visibility of the push notification
                showNotification = true,
                // Allows to customize the retention period of collected data
                retentionPeriod = RetentionManager.Period.ONE_HOUR
            ),
            // The max body content length in bytes, after this responses will be truncated.
            maxContentLength = 10500L,
            // List of headers to replace with ** in the Chucker UI
            headersToRedact = exclusionHeaders
        )
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