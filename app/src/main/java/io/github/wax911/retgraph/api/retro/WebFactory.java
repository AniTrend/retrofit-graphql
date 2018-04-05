package io.github.wax911.retgraph.api.retro;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import io.github.wax911.retgraph.BuildConfig;
import io.github.wax911.retgraph.api.converter.GraphQLConverter;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by max on 2018/04/05.
 * Retrofit service factory
 */
public class WebFactory {

    private final static Gson gson = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .setLenient().create();

    private final static Retrofit.Builder githubBuilder = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl("https://api.githunt.com/graphql");

    private static Retrofit mRetrofit;

    /**
     * Generates retrofit service classes in a background thread
     * and handles creation of API tokens or renewal of them
     * <br/>
     *
     * @param serviceClass The interface class to use such as
     *
     * @param context A valid application, fragment or activity context but must be application context
     */
    public static <S> S createService(@NonNull Class<S> serviceClass, Context context) {
        if(mRetrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .readTimeout(35, TimeUnit.SECONDS)
                    .connectTimeout(35, TimeUnit.SECONDS);

            if(BuildConfig.DEBUG) {
                HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClient.addInterceptor(httpLoggingInterceptor);
            }

            // add our custom converter to our http retrofit builder
            mRetrofit = githubBuilder.client(httpClient.build())
                    .addConverterFactory(GraphQLConverter.create(context))
                    .build();
        }
        return mRetrofit.create(serviceClass);
    }
}
