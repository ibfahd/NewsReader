package com.yourname.newsreader.data.repository

import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [NewsRepository] for ViewModel unit tests.
 *
 * ─── Fake vs Mock ─────────────────────────────────────────────────────────────
 * Mock (MockK/Mockito): stub individual calls, verify interactions.
 *   → Good for fine-grained interaction testing.
 *
 * Fake (this class): a hand-written, lightweight working implementation.
 *   → Better for repositories because:
 *     • It honours Flow semantics (state changes propagate to collectors).
 *     • It makes the test scenario explicit and readable.
 *     • It doesn't break if the interface signature shifts slightly.
 *
 * Use [FakeNewsRepository] for ViewModel tests where reactive state matters.
 * Use MockK when you only need to verify that a method was called.
 *
 * ─── Usage ────────────────────────────────────────────────────────────────────
 *   val repo = FakeNewsRepository()
 *   repo.setArticles(listOf(article1, article2))
 *   val vm = ArticleListViewModel(repo, SavedStateHandle())
 *   vm.uiState.test { ... }
 */
class FakeNewsRepository : NewsRepository {

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())

    // ── Test controls ──────────────────────────────────────────────────────────

    /** Seed the article list (triggers reactive updates to all collectors). */
    fun setArticles(articles: List<Article>) { _articles.value = articles }

    /** Cause [getArticles] to throw on the next collection. */
    var shouldThrowOnGetArticles = false

    /** Cause [refreshArticles] to throw on the next call. */
    var shouldThrowOnRefresh = false

    /** How many times [refreshArticles] was called — for verification. */
    var refreshCallCount = 0
        private set

    // ── NewsRepository ─────────────────────────────────────────────────────────

    override fun getArticles(category: Category?): Flow<List<Article>> {
        if (shouldThrowOnGetArticles) throw RuntimeException("Fake error: getArticles")
        return _articles.asStateFlow().map { articles ->
            if (category != null) articles.filter { it.category == category } else articles
        }
    }

    override suspend fun getArticleById(articleId: String): Article? =
        _articles.value.find { it.id == articleId }

    override suspend fun refreshArticles() {
        refreshCallCount++
        if (shouldThrowOnRefresh) throw RuntimeException("Fake error: refreshArticles")
        // The fake doesn't change _articles automatically — tests control state explicitly.
    }

    override suspend fun toggleFavorite(articleId: String, isFavorite: Boolean) {
        _favorites.value = if (isFavorite) _favorites.value + articleId
        else _favorites.value - articleId
    }

    override fun getFavoriteIds(): Flow<Set<String>> = _favorites.asStateFlow()
}