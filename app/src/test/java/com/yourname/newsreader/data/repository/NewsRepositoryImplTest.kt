package com.yourname.newsreader.data.repository

import app.cash.turbine.test
import com.yourname.newsreader.data.local.ArticleDao
import com.yourname.newsreader.data.local.ArticleEntity
import com.yourname.newsreader.data.local.toDomain
import com.yourname.newsreader.data.model.Category
import com.yourname.newsreader.data.preferences.UserPreferencesDataStore
import com.yourname.newsreader.data.remote.RemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [NewsRepositoryImpl].
 *
 * ─── Strategy ─────────────────────────────────────────────────────────────────
 * We mock all three dependencies (DAO, remote, preferences) with MockK.
 * This isolates the repository's own logic from I/O:
 *   - No SQLite reads/writes
 *   - No DataStore file I/O
 *   - No network calls
 *
 * ─── What we're testing ───────────────────────────────────────────────────────
 *   ✓ Correct delegation to DAO and remote source
 *   ✓ Entity → Domain model mapping
 *   ✓ In-memory cache prevents redundant DAO calls
 *   ✓ refreshArticles writes to DAO and updates timestamp
 *   ✓ Error propagation from the remote source
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewsRepositoryImplTest {

    private lateinit var articleDao: ArticleDao
    private lateinit var remoteDataSource: RemoteDataSource
    private lateinit var userPreferences: UserPreferencesDataStore
    private lateinit var repository: NewsRepositoryImpl

    private val testEntity = ArticleEntity(
        id = "1", title = "Quantum Computing", description = "Desc", content = "Content",
        author = "Dr. Chen", publishedAt = 1_000_000L, imageUrl = null,
        category = Category.TECHNOLOGY.name, source = "Tech Daily", url = "https://test.com/1"
    )

    @Before
    fun setup() {
        articleDao = mockk(relaxed = true)
        remoteDataSource = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)

        every { articleDao.getArticles(any()) } returns flowOf(listOf(testEntity))
        every { userPreferences.getFavoriteIds() } returns flowOf(emptySet())

        repository = NewsRepositoryImpl(articleDao, remoteDataSource, userPreferences)
    }

    // ── getArticles ────────────────────────────────────────────────────────────

    @Test
    fun `getArticles maps entity to domain model correctly`() = runTest {
        repository.getArticles(null).test {
            val articles = awaitItem()
            assertEquals(1, articles.size)
            assertEquals("1", articles[0].id)
            assertEquals("Quantum Computing", articles[0].title)
            assertEquals(Category.TECHNOLOGY, articles[0].category)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getArticles passes category name to DAO`() = runTest {
        repository.getArticles(Category.BUSINESS).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { articleDao.getArticles("BUSINESS") }
    }

    @Test
    fun `getArticles with null category passes null to DAO`() = runTest {
        repository.getArticles(null).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { articleDao.getArticles(null) }
    }

    // ── getArticleById ─────────────────────────────────────────────────────────

    @Test
    fun `getArticleById returns mapped domain model`() = runTest {
        coEvery { articleDao.getArticleById("1") } returns testEntity
        val result = repository.getArticleById("1")
        assertEquals("1", result?.id)
        assertEquals(Category.TECHNOLOGY, result?.category)
    }

    @Test
    fun `getArticleById returns null when article missing`() = runTest {
        coEvery { articleDao.getArticleById("999") } returns null
        assertNull(repository.getArticleById("999"))
    }

    @Test
    fun `getArticleById uses in-memory cache on second call`() = runTest {
        coEvery { articleDao.getArticleById("1") } returns testEntity

        repository.getArticleById("1")  // first call → hits DAO
        repository.getArticleById("1")  // second call → hits memory cache

        coVerify(exactly = 1) { articleDao.getArticleById("1") }  // DAO called only once
    }

    // ── refreshArticles ────────────────────────────────────────────────────────

    @Test
    fun `refreshArticles fetches from remote and inserts to DAO`() = runTest {
        coEvery { remoteDataSource.fetchArticles() } returns listOf(testEntity.toDomain())

        repository.refreshArticles()

        coVerify { remoteDataSource.fetchArticles() }
        coVerify { articleDao.insertArticles(any()) }
        coVerify { userPreferences.updateLastRefreshTimestamp() }
    }

    @Test
    fun `refreshArticles propagates remote exceptions to caller`() = runTest {
        coEvery { remoteDataSource.fetchArticles() } throws RuntimeException("Network timeout")

        val result = runCatching { repository.refreshArticles() }

        assertTrue(result.isFailure)
        assertEquals("Network timeout", result.exceptionOrNull()?.message)
    }

    // ── Favourites ─────────────────────────────────────────────────────────────

    @Test
    fun `toggleFavorite delegates to UserPreferencesDataStore`() = runTest {
        repository.toggleFavorite("1", true)
        coVerify { userPreferences.toggleFavorite("1", true) }
    }

    @Test
    fun `getFavoriteIds returns Flow from UserPreferencesDataStore`() = runTest {
        every { userPreferences.getFavoriteIds() } returns flowOf(setOf("1", "3"))

        repository.getFavoriteIds().test {
            assertEquals(setOf("1", "3"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
