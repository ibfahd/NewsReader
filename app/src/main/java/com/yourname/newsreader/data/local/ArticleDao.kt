package com.yourname.newsreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) — the interface between Kotlin and SQLite.
 *
 * ─── How Room works ───────────────────────────────────────────────────────────
 * Room reads this interface at compile time (via KSP), validates every SQL
 * query against the current schema, then generates a concrete ArticleDao_Impl
 * class. If a query references a missing column, the build fails — no surprises
 * at runtime.
 *
 * ─── Flow return types ────────────────────────────────────────────────────────
 * Functions returning Flow<T> are *reactive queries*:
 *   - Room observes the table for changes automatically.
 *   - Every INSERT/UPDATE/DELETE on "articles" triggers a re-emission.
 *   - The Repository passes this Flow up to the ViewModel, which drives the UI.
 *   - No polling, no manual invalidation needed.
 *
 * ─── Suspend functions ────────────────────────────────────────────────────────
 * One-shot writes (insertArticles, deleteAllArticles) are suspend functions.
 * Room dispatches them on its own IO Executor — they never block the main thread.
 */
@Dao
interface ArticleDao {

    /**
     * Observe articles ordered newest-first, with optional category filter.
     *
     * SQL idiom: `:category IS NULL OR category = :category`
     *   - Null argument  → first condition is TRUE  → all rows returned
     *   - "TECHNOLOGY"   → second condition filters  → only matching rows
     *
     * Room generates a prepared statement that safely binds the Kotlin null
     * to SQL NULL — no string interpolation, no SQL injection risk.
     */
    @Query("""
        SELECT * FROM articles
        WHERE (:category IS NULL OR category = :category)
        ORDER BY publishedAt DESC
    """)
    fun getArticles(category: String?): Flow<List<ArticleEntity>>

    /**
     * Fetch a single article snapshot (no reactivity needed for the detail screen).
     * Returns null if the article doesn't exist in the local cache.
     */
    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: String): ArticleEntity?

    /**
     * Batch-insert articles from the network.
     *
     * [OnConflictStrategy.REPLACE] implements the offline-first pattern:
     *   - New articles → inserted.
     *   - Existing articles (same primary key) → replaced with fresh data.
     *   - No "UNIQUE constraint failed" errors on re-fetch.
     *
     * Performance: Room wraps the entire list in a single SQLite transaction
     * automatically, making this ~10× faster than one-by-one inserts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    /**
     * Count articles in the table.
     * Used by the repository to decide whether an initial network seed is needed.
     */
    @Query("SELECT COUNT(*) FROM articles WHERE (:category IS NULL OR category = :category)")
    suspend fun countArticles(category: String?): Int

    /**
     * Wipe all articles — used for full refresh or account sign-out.
     */
    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()
}