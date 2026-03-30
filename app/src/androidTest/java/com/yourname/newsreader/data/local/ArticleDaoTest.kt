package com.yourname.newsreader.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.yourname.newsreader.data.model.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [ArticleDao].
 *
 * ─── Why instrumented (not unit) tests? ───────────────────────────────────────
 * Room's generated code uses Android's SQLite implementation. There's no JVM
 * substitute — these tests must run on a real device or emulator.
 *
 * ─── In-memory database ───────────────────────────────────────────────────────
 * [Room.inMemoryDatabaseBuilder] creates a database that:
 *   - Lives entirely in RAM — no files written to disk.
 *   - Is automatically destroyed when the test process ends.
 *   - Supports [allowMainThreadQueries] — safe in tests, never in production.
 *   - Runs faster than a file-based DB (no fsync needed).
 *
 * Each test gets a fresh database (setup/tearDown), so tests are fully isolated.
 *
 * ─── Turbine ──────────────────────────────────────────────────────────────────
 * app.cash.turbine simplifies asserting on Flow emissions:
 *   flow.test { val item = awaitItem(); assertEquals(expected, item) }
 * Without Turbine, you'd need a runBlocking + launch + Channel — much more boilerplate.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {

    private lateinit var database: NewsDatabase
    private lateinit var dao: ArticleDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NewsDatabase::class.java
        )
            .allowMainThreadQueries() // Acceptable ONLY in tests
            .build()
        dao = database.articleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun entity(
        id: String,
        category: String = Category.TECHNOLOGY.name,
        publishedAt: Long = System.currentTimeMillis()
    ) = ArticleEntity(
        id = id, title = "Title $id", description = "Desc", content = "Content",
        author = "Author", publishedAt = publishedAt, imageUrl = null,
        category = category, source = "Source", url = "https://example.com/$id"
    )

    // ── Insert & Query ─────────────────────────────────────────────────────────

    @Test
    fun insertAndQueryAll() = runTest {
        dao.insertArticles(listOf(entity("1"), entity("2")))
        dao.getArticles(null).test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filterByCategory() = runTest {
        dao.insertArticles(listOf(
            entity("1", Category.TECHNOLOGY.name),
            entity("2", Category.BUSINESS.name),
            entity("3", Category.TECHNOLOGY.name)
        ))
        dao.getArticles(Category.TECHNOLOGY.name).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.category == Category.TECHNOLOGY.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun orderedByPublishedAtDescending() = runTest {
        dao.insertArticles(listOf(entity("old", publishedAt = 1000L), entity("new", publishedAt = 9000L)))
        dao.getArticles(null).test {
            val result = awaitItem()
            assertEquals("new", result[0].id) // newest first
            assertEquals("old", result[1].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Conflict resolution ────────────────────────────────────────────────────

    @Test
    fun replaceOnConflict_updatesExistingRow() = runTest {
        dao.insertArticles(listOf(entity("1").copy(title = "Original")))
        dao.insertArticles(listOf(entity("1").copy(title = "Updated")))

        dao.getArticles(null).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Updated", result[0].title) // replaced, not duplicated
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Single article ─────────────────────────────────────────────────────────

    @Test
    fun getById_returnsCorrectArticle() = runTest {
        dao.insertArticles(listOf(entity("42")))
        val result = dao.getArticleById("42")
        assertNotNull(result)
        assertEquals("42", result!!.id)
    }

    @Test
    fun getById_returnsNullWhenMissing() = runTest {
        assertNull(dao.getArticleById("ghost"))
    }

    // ── Count & Delete ─────────────────────────────────────────────────────────

    @Test
    fun countArticles() = runTest {
        dao.insertArticles(listOf(
            entity("1", Category.TECHNOLOGY.name),
            entity("2", Category.BUSINESS.name)
        ))
        assertEquals(2, dao.countArticles(null))
        assertEquals(1, dao.countArticles(Category.TECHNOLOGY.name))
        assertEquals(0, dao.countArticles(Category.HEALTH.name))
    }

    @Test
    fun deleteAll_emptiesTable() = runTest {
        dao.insertArticles(listOf(entity("1"), entity("2")))
        dao.deleteAllArticles()

        dao.getArticles(null).test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Reactivity ────────────────────────────────────────────────────────────

    @Test
    fun flowReEmitsAfterInsert() = runTest {
        dao.getArticles(null).test {
            assertEquals(0, awaitItem().size)          // initial empty emission

            dao.insertArticles(listOf(entity("1")))
            assertEquals(1, awaitItem().size)          // reactive re-emission

            dao.insertArticles(listOf(entity("2")))
            assertEquals(2, awaitItem().size)          // another re-emission

            cancelAndIgnoreRemainingEvents()
        }
    }
}