package com.yourname.newsreader.data.repository

import androidx.paging.PagingData
import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Ch.7 addition: [getPagedArticles] — returns a [Flow<PagingData<Article>>]
 * for the infinite-scrolling article list.
 *
 * The rest of the interface is unchanged. [getArticles] is still used by the
 * detail screen (single article lookup) and the repository's own logic.
 * [refreshArticles] is still used for explicit pull-to-refresh on non-paged
 * content.
 */
interface NewsRepository {

    fun getArticles(category: Category? = null): Flow<List<Article>>

    /**
     * Returns a paged stream of articles for [category].
     *
     * The [Flow<PagingData<Article>>] combines:
     *   - A Room [PagingSource] (reads local data page by page).
     *   - An [ArticleRemoteMediator] (fetches from network and writes to Room).
     *
     * The UI collects this with [collectAsLazyPagingItems], which provides load
     * state, retry, and refresh — no manual state management required.
     *
     * @param category Filter to a specific category, or null for all categories.
     */
    fun getPagedArticles(category: Category?): Flow<PagingData<Article>>

    suspend fun getArticleById(articleId: String): Article?

    suspend fun refreshArticles()

    suspend fun toggleFavorite(articleId: String, isFavorite: Boolean)

    fun getFavoriteIds(): Flow<Set<String>>
}