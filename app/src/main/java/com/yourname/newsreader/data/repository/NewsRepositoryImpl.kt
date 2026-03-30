package com.yourname.newsreader.data.repository

import com.yourname.newsreader.data.local.ArticleDao
import com.yourname.newsreader.data.local.toDomain
import com.yourname.newsreader.data.local.toEntity
import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import com.yourname.newsreader.data.preferences.UserPreferencesDataStore
import com.yourname.newsreader.data.remote.RemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [NewsRepository].
 * Replaces [MockNewsRepository] from Chapter 5.
 *
 * ─── Offline-First Architecture ───────────────────────────────────────────────
 * The Room database is the SINGLE SOURCE OF TRUTH. The UI never reads from
 * the network directly — it only observes Room, and the Repository syncs Room
 * with the network in the background.
 *
 *   ┌──────┐     Flow      ┌──────┐   reactive   ┌────────────────┐
 *   │  UI  │ ─────────────▶│  VM  │◀────────────▶│ NewsRepository │
 *   └──────┘               └──────┘              └────────┬───────┘
 *                                                          │
 *                                    ┌─────────────────────┤
 *                                    ▼                     ▼
 *                              ┌──────────┐         ┌──────────────┐
 *                              │   Room   │◀───────▶│ RemoteSource │
 *                              └──────────┘  writes └──────────────┘
 *
 * ─── Caching layers ───────────────────────────────────────────────────────────
 *   L1 — In-memory LRU   fast O(1) dict lookup    article detail screen
 *   L2 — Room / SQLite   persistent across restarts  article list
 *   L3 — Network         source of fresh content   triggered by refresh
 *
 * ─── Hilt annotations ─────────────────────────────────────────────────────────
 * @Singleton  — one instance per app; the LRU cache lives in this instance.
 * @Inject     — Hilt resolves: ArticleDao (from DatabaseModule),
 *               RemoteDataSource (bound to MockRemoteDataSource in RepositoryModule),
 *               UserPreferencesDataStore (injectable directly via @Singleton + @Inject).
 */
@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val remoteDataSource: RemoteDataSource,
    private val userPreferences: UserPreferencesDataStore
) : NewsRepository {

    // ─── L1: In-memory LRU cache ──────────────────────────────────────────────
    // A LinkedHashMap in access-order mode acts as a perfect LRU cache:
    // - removeEldestEntry evicts the least-recently-used entry when full.
    // - Cost: a few KB of memory for up to 50 articles.
    // - Benefit: O(1) lookup for the detail screen — no DB round-trip.
    //
    // Note: Not thread-safe by itself. Since all repository calls are on
    // coroutine dispatchers (not raw threads), single-coroutine access is fine.
    // For multi-thread safety, wrap in Collections.synchronizedMap() or use
    // a concurrent map (at the cost of slightly higher overhead).
    private val memoryCache = object : LinkedHashMap<String, Article>(
        32, 0.75f, /* accessOrder = */ true
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Article>) =
            size > MAX_MEMORY_CACHE_SIZE
    }

    // ─── Reactive article stream ──────────────────────────────────────────────

    /**
     * Observe articles as a reactive Flow from Room.
     *
     * Key behaviour:
     * - Emits immediately with cached data (instant UI, works offline).
     * - Re-emits automatically whenever [refreshArticles] writes new data to Room.
     * - The ViewModel never needs to know whether data came from network or cache.
     *
     * [category] is passed directly to the DAO query — the SQL WHERE clause
     * handles null (= all categories) vs a specific value.
     */
    override fun getArticles(category: Category?): Flow<List<Article>> =
        articleDao.getArticles(category?.name)
            .map { entities ->
                entities.map { entity ->
                    entity.toDomain().also { article ->
                        // "Free" L1 warm-up: populate cache on every DB read.
                        // The detail screen benefits from this immediately.
                        memoryCache[article.id] = article
                    }
                }
            }

    // ─── Single-article lookup ────────────────────────────────────────────────

    /**
     * Fetch one article, checking cache layers in order:
     *   L1 (memory)  → L2 (Room).
     *
     * No network call here — if the article isn't in L1 or L2, it means the
     * detail screen was opened before the initial refresh completed (edge case).
     * In that scenario, null is returned and the detail screen shows an error.
     */
    override suspend fun getArticleById(articleId: String): Article? {
        // L1: memory hit — instant, no coroutine suspension
        memoryCache[articleId]?.let { return it }

        // L2: Room hit — brief suspension, but no network I/O
        return articleDao.getArticleById(articleId)
            ?.toDomain()
            ?.also { article -> memoryCache[article.id] = article } // warm L1
    }

    // ─── Network refresh ──────────────────────────────────────────────────────

    /**
     * Fetch fresh articles from the remote source and persist them to Room.
     *
     * After this function returns:
     *   - Room contains the latest articles.
     *   - The reactive Flow from [getArticles] automatically re-emits.
     *   - The ViewModel's UI state updates without any manual wiring.
     *   - The memory cache is cleared so the next detail-screen open reads
     *     fresh data from Room (re-warming the cache).
     *
     * Performance: [insertArticles] wraps all rows in a single SQLite
     * transaction — roughly 10× faster than individual inserts for large lists.
     *
     * @throws Exception if the network call fails (caller handles this).
     */
    override suspend fun refreshArticles() {
        val remoteArticles = remoteDataSource.fetchArticles()
        articleDao.insertArticles(remoteArticles.map { it.toEntity() })
        memoryCache.clear()
        userPreferences.updateLastRefreshTimestamp()
    }

    // ─── Favourites (delegated to DataStore) ──────────────────────────────────

    /** Delegates to DataStore's atomic, coroutine-safe write. */
    override suspend fun toggleFavorite(articleId: String, isFavorite: Boolean) =
        userPreferences.toggleFavorite(articleId, isFavorite)

    /** Exposes DataStore's reactive favourites Flow directly. */
    override fun getFavoriteIds(): Flow<Set<String>> =
        userPreferences.getFavoriteIds()

    companion object {
        /** Maximum articles held in the in-memory LRU cache. */
        private const val MAX_MEMORY_CACHE_SIZE = 50
    }
}