package com.yourname.newsreader.di

import com.yourname.newsreader.data.remote.MockRemoteDataSource
import com.yourname.newsreader.data.remote.RemoteDataSource
import com.yourname.newsreader.data.repository.NewsRepository
import com.yourname.newsreader.data.repository.NewsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module: binds interfaces to their concrete implementations.
 *
 * ─── @Binds vs @Provides ──────────────────────────────────────────────────────
 *
 * @Provides — you write the construction logic:
 *   fun provideRepo(dao: ArticleDao, ...): NewsRepository = NewsRepositoryImpl(dao, ...)
 *
 * @Binds   — you just declare the mapping:
 *   abstract fun bindRepo(impl: NewsRepositoryImpl): NewsRepository
 *
 * Prefer @Binds: it's more efficient (Hilt doesn't generate delegation code)
 * and more expressive ("use THIS class when asked for THAT interface").
 *
 * @Binds functions must be abstract — and therefore the Module must be an
 * abstract class, not an object.
 *
 * ─── The power of this pattern ────────────────────────────────────────────────
 * No code outside this Module imports MockRemoteDataSource or NewsRepositoryImpl.
 * All other code depends only on the interfaces. To swap in Chapter 7:
 *
 *   // Before:
 *   abstract fun bindRemoteDataSource(impl: MockRemoteDataSource): RemoteDataSource
 *
 *   // After (Chapter 7):
 *   abstract fun bindRemoteDataSource(impl: RetrofitRemoteDataSource): RemoteDataSource
 *
 * That's the only change needed. Every class that depends on RemoteDataSource
 * automatically gets the Retrofit implementation — zero refactoring.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * "When code asks for [NewsRepository], provide [NewsRepositoryImpl]."
     * @Singleton keeps the LRU memory cache alive for the app's lifetime.
     */
    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        impl: NewsRepositoryImpl
    ): NewsRepository

    /**
     * "When code asks for [RemoteDataSource], provide [MockRemoteDataSource]."
     * Chapter 7: change this to RetrofitRemoteDataSource.
     */
    @Binds
    @Singleton
    abstract fun bindRemoteDataSource(
        impl: MockRemoteDataSource
    ): RemoteDataSource
}