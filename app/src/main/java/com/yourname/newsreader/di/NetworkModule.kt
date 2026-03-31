package com.yourname.newsreader.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yourname.newsreader.BuildConfig
import com.yourname.newsreader.data.remote.NewsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt Module: provides the entire networking stack.
 *
 * The dependency chain built here is:
 *   Moshi → MoshiConverterFactory ─┐
 *   OkHttpClient ──────────────────┤→ Retrofit → NewsApiService
 *
 * Every object in this chain is @Singleton: creating an OkHttpClient or Retrofit
 * instance is expensive (connection pools, thread pools, reflection). There
 * should be exactly one of each for the app's lifetime.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Moshi — the JSON parser.
     *
     * We add [KotlinJsonAdapterFactory] as a fallback for any classes that are
     * NOT annotated with @JsonClass(generateAdapter = true). In this app, all
     * our DTOs use codegen, so this factory is rarely invoked — but it prevents
     * crashes if a third-party model is ever deserialised without an annotation.
     *
     * The order matters: KSP-generated adapters are registered first (implicitly),
     * then this factory handles anything remaining.
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * OkHttpClient — the HTTP engine.
     *
     * Three interceptors are configured here:
     *
     * 1. API Key Interceptor (always active):
     *    Adds the "apiKey" query parameter to every outgoing request. This is
     *    more maintainable than adding @Query("apiKey") to every service method.
     *    If the auth mechanism changes (e.g. to a header), only this block needs
     *    updating.
     *
     * 2. HttpLoggingInterceptor (debug builds only):
     *    Logs the complete request URL, headers, and response body to Logcat.
     *    Level.BODY is the most verbose level — it includes everything. This is
     *    invaluable for debugging but MUST NOT be included in release builds,
     *    because it could log the API key and user data to Logcat where other
     *    apps with READ_LOGS permission could see it.
     *
     * Timeout strategy:
     *    - connectTimeout: how long to wait for a TCP connection to be established.
     *    - readTimeout: how long to wait for the server to send a response body.
     *    - writeTimeout: how long to wait to send the request body.
     *    All three are separate because each phase can fail independently.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

            // API key interceptor — inject the key as a query parameter
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val urlWithKey = originalRequest.url.newBuilder()
                    .addQueryParameter("apiKey", BuildConfig.NEWS_API_KEY)
                    .build()
                chain.proceed(originalRequest.newBuilder().url(urlWithKey).build())
            }

        // Only add logging in debug builds.
        // BuildConfig.DEBUG is false in release builds even if isMinifyEnabled=false.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }

        return builder.build()
    }

    /**
     * Retrofit — the HTTP client abstraction layer.
     *
     * [MoshiConverterFactory] tells Retrofit to use Moshi for all response body
     * deserialisation. When a suspend function annotated with @GET returns a DTO,
     * Retrofit pipes the raw JSON through Moshi automatically.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(NewsApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    /**
     * The Retrofit service interface implementation.
     *
     * [Retrofit.create] uses dynamic proxies to generate a concrete class that
     * implements [NewsApiService] at runtime. The generated implementation
     * handles the HTTP calls, response parsing, and coroutine suspension.
     */
    @Provides
    @Singleton
    fun provideNewsApiService(retrofit: Retrofit): NewsApiService =
        retrofit.create(NewsApiService::class.java)
}