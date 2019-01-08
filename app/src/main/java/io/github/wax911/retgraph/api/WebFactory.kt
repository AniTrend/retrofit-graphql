package io.github.wax911.retgraph.api

import android.content.Context

import java.util.concurrent.TimeUnit

import io.github.wax911.library.converter.GraphConverter
import io.github.wax911.retgraph.BuildConfig
import io.github.wax911.retgraph.api.retro.converter.GitHuntConverter
import io.github.wax911.retgraph.api.retro.request.IndexModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Created by max on 2018/04/05.
 * Retrofit service factory
 */
class WebFactory private constructor(context: Context?){

    private val mRetrofit: Retrofit by lazy {
        val httpClient = OkHttpClient.Builder()
                .readTimeout(35, TimeUnit.SECONDS)
                .connectTimeout(35, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.HEADERS)
            httpClient.addInterceptor(httpLoggingInterceptor)
        }

        Retrofit.Builder()
                .client(httpClient.build())
                .baseUrl("https://api.githunt.com/")
                // the default converter
                //.addConverterFactory(GraphConverter.create(context))
                // or
                .addConverterFactory(GitHuntConverter.create(context))
                .build()
    }

    /**
     * Generates retrofit service classes
     *
     * @param serviceClass The interface class method representing your request to use
     * @see IndexModel methods
     */
    fun <S> createService(serviceClass: Class<S>): S = mRetrofit.create(serviceClass)

    companion object {

        @Volatile private var instance: WebFactory? = null
        private val lock = Any()

        fun getInstance(context: Context?): WebFactory {
            val singleton = instance
            if (singleton != null)
                return singleton

            return synchronized(lock) {
                val init = instance
                if (init != null)
                    init
                else {
                    val created = WebFactory(context)
                    instance = created
                    created
                }
            }
        }
    }
}
