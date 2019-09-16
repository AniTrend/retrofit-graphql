package io.github.wax911.retgraph.koin

import io.github.wax911.retgraph.BuildConfig
import io.github.wax911.retgraph.api.WebFactory
import io.github.wax911.retgraph.api.retro.converter.GitHuntConverter
import io.github.wax911.retgraph.api.retro.request.IndexModel
import io.github.wax911.retgraph.viewmodel.TrendingViewModel
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

private val networkModule = module {
    single {
        val httpClient = OkHttpClient.Builder()
                .readTimeout(35, TimeUnit.SECONDS)
                .connectTimeout(35, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.HEADERS)
            httpClient.addInterceptor(httpLoggingInterceptor)
        }

        val converter = GitHuntConverter.create(
                context = androidContext()
        )

        Retrofit.Builder()
                .client(httpClient.build())
                .baseUrl("https://api.githunt.com/")
                // the default converter
                //.addConverterFactory(GraphConverter.create(context))
                // or
                .addConverterFactory(converter)
                .build()
    }
}

private val factoryModule = module {
    factory {
        WebFactory(
                retrofit = get()
        )
    }
}

private val viewModelModule = module {
    viewModel {
        TrendingViewModel(
                service = get<WebFactory>().create(IndexModel::class.java),
                dispatcher = Dispatchers.IO
        )
    }
}

val appModules = listOf(networkModule, factoryModule, viewModelModule)