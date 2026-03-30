package com.yourname.newsreader.data.remote

import com.yourname.newsreader.data.model.Article

/**
 * Abstraction over the network layer.
 *
 * ─── Why an interface? ────────────────────────────────────────────────────────
 * The Repository depends on this interface, not on any concrete class. This
 * decoupling enables:
 *
 *   Development/testing  → MockRemoteDataSource (this chapter)
 *   Production           → RetrofitRemoteDataSource (Chapter 7)
 *
 * Swapping is a one-line change in RepositoryModule — no other file changes.
 *
 * ─── In a production app ──────────────────────────────────────────────────────
 * The real implementation would be:
 *
 *   class RetrofitRemoteDataSource @Inject constructor(
 *       private val api: NewsApiService        // Retrofit interface
 *   ) : RemoteDataSource {
 *       override suspend fun fetchArticles(): List<Article> =
 *           api.getTopHeadlines().articles.map { it.toDomain() }
 *   }
 *
 * Note: the real source would return network DTOs (NewsArticleDto) and map
 * them to domain Articles. We skip the DTO layer here for brevity.
 */
interface RemoteDataSource {

    /**
     * Fetch the latest articles from the remote source.
     *
     * @return List of domain [Article] objects, newest first.
     * @throws Exception on network failure, timeout, or parse error.
     *   The Repository catches this and handles it appropriately.
     */
    suspend fun fetchArticles(): List<Article>
}