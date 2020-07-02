package co.anitrend.retrofit.graphql.core.koin

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.core.settings.Settings
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.util.CoilUtils
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
                CoilUtils.createDefaultCache(
                    androidApplication()
                )
            ).build()
    }
    factory<ImageLoaderFactory> {
        object : ImageLoaderFactory {
            override fun newImageLoader(): ImageLoader {
                return ImageLoader.Builder(androidContext())
                    .crossfade(true)
                    .allowHardware(true)
                    .bitmapPoolPercentage(0.35)
                    .okHttpClient {
                        get(named("coilOkHttp"))
                    }
                    .componentRegistry {
                        if (SDK_INT >= P) add(ImageDecoderDecoder())
                        else add(GifDecoder())
                    }
                    .build()
            }
        }
    }
}

val coreModules = listOf(coreModule, coilModules)