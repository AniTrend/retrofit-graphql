package co.anitrend.retrofit.graphql.core.koin

import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.core.settings.Settings
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private val coreModule = module {
    factory {
        Settings(
            androidContext()
        )
    } binds(Settings.BINDINGS)
    single {
        SupportDispatchers()
    }
}

private val coilModules = module {
    factory(named("coilOkHttp")) {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
            )
            .cache(
                Cache(androidApplication().cacheDir, 50 * 1024 * 1024)
            ).build()
    }
    factory<ImageLoaderFactory> {
        ImageLoaderFactory {
            ImageLoader.Builder(androidContext())
                .crossfade(true)
                .allowHardware(true)
                .okHttpClient {
                    get(named("coilOkHttp"))
                }
                .build()
        }
    }
}

val coreModules = listOf(coreModule, coilModules)