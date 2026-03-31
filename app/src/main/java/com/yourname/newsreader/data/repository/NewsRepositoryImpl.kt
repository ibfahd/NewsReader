package com.yourname.newsreader.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.yourname.newsreader.data.local.ArticleDao
import com.yourname.newsreader.data.local.NewsDatabase
import com.yourname.newsreader.data.local.toDomain
import com.yourname.newsreader.data.local.toEntity
import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import com.yourname.newsreader.data.paging.ArticleRemoteMediator
import com.yourname.newsreader.data.preferences.UserPreferencesDataStore
import com.yourname.newsreader.data.remote.NewsApiService
import com.yourname.newsreader.data.remote.RemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ch.7 changes:
 *   - [NewsApiService] added as a constructor parameter (used by RemoteMediator).
 *   - [NewsDatabase] added as a constructor parameter (needed by RemoteMediator
 *     for [withTransaction], which requires the full database object).
 *   - [getPagedArticles] implemented using [Pager] + [ArticleRemoteMediator].
 *   - The in-memory LRU cache and all other behaviour are unchanged.
 */
@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val db: NewsDatabase,
    private val remoteDataSource: RemoteDataSource,
    private val apiService: NewsApiService,
    private val userPreferences: UserPreferencesDataStore
) : NewsRepository {

    // ─── L1: In-memory LRU cache (unchanged from Ch.6) ───────────────────────
    private val memoryCache = object : LinkedHashMap<String, Article>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Article>) =
            size > MAX_MEMORY_CACHE_SIZE
    }

    override fun getArticles(category: Category?): Flow<List<Article>> =
        articleDao.getArticles(category?.name).map { entities ->
            entities.map { entity ->
                entity.toDomain().also { memoryCache[it.id] = it }
            }
        }

    /**
     * Creates a [Pager] that wires together the [ArticleRemoteMediator]
     * and Room's [PagingSource].
     *
     * ─── How Pager works ──────────────────────────────────────────────────────
     * [Pager] is the entry point for Paging 3. It accepts three things:
     *
     * 1. [PagingConfig] — controls how pages are loaded:
     *    - [pageSize]: how many items to load per batch. 20 fits NewsAPI's
     *      free tier comfortably.
     *    - [enablePlaceholders]: if true, the list shows null items for
     *      positions not yet loaded (useful for fixed-size grids). We set it
     *      to false because article lists are variable-height.
     *    - [prefetchDistance]: Paging starts fetching the next page when the
     *      user is this many items from the end. Default is pageSize, which
     *      means loading starts before the user actually runs out of items.
     *
     * 2. [remoteMediator]: our [ArticleRemoteMediator] — handles network fetches
     *    and Room writes.
     *
     * 3. [pagingSourceFactory]: a lambda that Room calls every time it needs a
     *    fresh [PagingSource] — for example, after the mediator writes new rows
     *    (which invalidates the current PagingSource).
     *
     * The [.flow] extension converts the Pager into a cold [Flow<PagingData<T>>].
     * The [.map { pagingData -> pagingData.map { it.toDomain() } }] transforms
     * each page's entities into domain objects as they are loaded, keeping Room
     * concerns out of the UI layer.
     */
    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedArticles(category: Category?): Flow<PagingData<Article>> =
        Pager(
            config = PagingConfig(
                pageSize = NewsApiService.PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = ArticleRemoteMediator(
                category = category,
                apiService = apiService,
                db = db
            ),
            pagingSourceFactory = { articleDao.getArticlesPagingSource(category?.name) }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override suspend fun getArticleById(articleId: String): Article? {
        memoryCache[articleId]?.let { return it }
        return articleDao.getArticleById(articleId)
            ?.toDomain()
            ?.also { memoryCache[it.id] = it }
    }

    override suspend fun refreshArticles() {
        val remoteArticles = remoteDataSource.fetchArticles()
        articleDao.insertArticles(remoteArticles.map { it.toEntity() })
        memoryCache.clear()
        userPreferences.updateLastRefreshTimestamp()
    }

    override suspend fun toggleFavorite(articleId: String, isFavorite: Boolean) =
        userPreferences.toggleFavorite(articleId, isFavorite)

    override fun getFavoriteIds(): Flow<Set<String>> =
        userPreferences.getFavoriteIds()

    companion object {
        private const val MAX_MEMORY_CACHE_SIZE = 50
    }
}