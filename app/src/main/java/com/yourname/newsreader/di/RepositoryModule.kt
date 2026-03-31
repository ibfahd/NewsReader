package com.yourname.newsreader.di

import com.yourname.newsreader.data.remote.RemoteDataSource
import com.yourname.newsreader.data.remote.RetrofitRemoteDataSource  // ← Chapter 7 swap
import com.yourname.newsreader.data.repository.NewsRepository
import com.yourname.newsreader.data.repository.NewsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The Chapter 7 change is exactly one line: MockRemoteDataSource → RetrofitRemoteDataSource.
 *
 * Every other class in the project — NewsRepositoryImpl, the ViewModels,
 * the UI — is completely unchanged. This is the payoff of depending on
 * the [RemoteDataSource] interface rather than any concrete class.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    /**
     * Chapter 6: MockRemoteDataSource
     * Chapter 7: RetrofitRemoteDataSource ← this single line is the entire swap
     */
    @Binds @Singleton
    abstract fun bindRemoteDataSource(impl: RetrofitRemoteDataSource): RemoteDataSource
}