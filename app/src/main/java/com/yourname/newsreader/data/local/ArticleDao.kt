package com.yourname.newsreader.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Ch.7 additions:
 *   - [getArticlesPagingSource] — returns a [PagingSource] instead of a [Flow],
 *     enabling Room to feed data directly into the Paging 3 library.
 *   - [deleteArticlesForCategory] — called by [ArticleRemoteMediator] on refresh
 *     to clear stale data for a specific category before inserting fresh pages.
 */
@Dao
interface ArticleDao {

    @Query("""
        SELECT * FROM articles
        WHERE (:category IS NULL OR category = :category)
        ORDER BY publishedAt DESC
    """)
    fun getArticles(category: String?): Flow<List<ArticleEntity>>

    /**
     * Returns a [PagingSource] — Paging 3's handle for reading data in pages.
     *
     * Room auto-generates the PagingSource implementation that loads
     * [PagingConfig.pageSize] rows at a time, starting from the most recent.
     * The Paging library calls this factory whenever it needs a fresh source
     * (e.g. after an invalidation triggered by a new insert from the mediator).
     *
     * The key difference from [getArticles]: PagingSource is a pull-based
     * "give me the next N rows" interface, while Flow is push-based "notify me
     * when anything changes". For large lists, PagingSource is dramatically
     * more memory efficient.
     */
    @Query("""
        SELECT * FROM articles
        WHERE (:category IS NULL OR category = :category)
        ORDER BY publishedAt DESC
    """)
    fun getArticlesPagingSource(category: String?): PagingSource<Int, ArticleEntity>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("SELECT COUNT(*) FROM articles WHERE (:category IS NULL OR category = :category)")
    suspend fun countArticles(category: String?): Int

    /**
     * Delete articles for a specific category on refresh.
     * Called by [ArticleRemoteMediator] inside a transaction alongside
     * [RemoteKeyDao.deleteRemoteKeysForCategory] to atomically clear
     * stale data before writing a fresh first page.
     */
    @Query("DELETE FROM articles WHERE (:category IS NULL AND category IS NULL) OR category = :category")
    suspend fun deleteArticlesForCategory(category: String?)

    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()
}