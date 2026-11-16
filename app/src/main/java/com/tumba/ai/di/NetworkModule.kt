package com.tumba.ai.di

import android.util.Log
import com.tumba.ai.BuildConfig
import com.tumba.ai.data.network.ModelDiscoveryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request()
            if (BuildConfig.DEBUG) {
                // Host and path only: query strings and headers can carry credentials.
                Log.d("TumbaHttp", "${request.method} ${request.url.host}${request.url.encodedPath}")
            }
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/")
        .client(client)
        .build()

    @Provides
    @Singleton
    fun provideModelDiscoveryApi(retrofit: Retrofit): ModelDiscoveryApi =
        retrofit.create(ModelDiscoveryApi::class.java)
}
