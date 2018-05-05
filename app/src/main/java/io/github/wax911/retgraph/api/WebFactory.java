package io.github.wax911.retgraph.api;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import io.github.wax911.library.converter.GraphConverter;
import io.github.wax911.retgraph.BuildConfig;
import io.github.wax911.retgraph.api.retro.request.IndexModel;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by max on 2018/04/05.
 * Retrofit service factory
 */
public class WebFactory {

    private static Retrofit mRetrofit;

    /**
     * Generates retrofit service classes
     *
     * @param serviceClass The interface class method representing your request to use
     *                     @see IndexModel methods
     * @param context A valid application, fragment or activity context
     */
    public static <S> S createService(@NonNull Class<S> serviceClass, Context context) {
        if(mRetrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .readTimeout(35, TimeUnit.SECONDS)
                    .connectTimeout(35, TimeUnit.SECONDS);

            if(BuildConfig.DEBUG) {
                HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.HEADERS);
                httpClient.addInterceptor(httpLoggingInterceptor);
            }

            // Note, we are not adding the default gson converter
            // because the GraphConverter will handle both body and parameter conversion
            mRetrofit = new Retrofit.Builder()
                    .client(httpClient.build())
                    .baseUrl("https://api.githunt.com/")
                    .addConverterFactory(GraphConverter.create(context))
                    .build();
        }
        return mRetrofit.create(serviceClass);
    }
}
