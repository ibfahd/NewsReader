package com.yourname.newsreader.ui.screens.articlelist

import com.yourname.newsreader.data.model.Category

/**
 * Ch.7: The state model is significantly simplified.
 *
 * In Chapter 6, [ArticleListState] was a sealed interface with Loading, Success,
 * and Error states because we managed all loading states manually. In Chapter 7,
 * Paging 3 manages loading states through [LazyPagingItems.loadState] —
 * a richer, more granular state that knows whether the initial load, a prepend,
 * or an append is in progress, and which operation errored.
 *
 * What this class now tracks is the UI state that Paging does NOT manage:
 *   - Which category filter is selected.
 *   - Which articles are favourited.
 *
 * The article list content itself is driven by [LazyPagingItems<Article>],
 * which is collected directly in the composable from the ViewModel's paging flow.
 */
data class ArticleListUiState(
    val selectedCategory: Category? = null,
    val favoriteIds: Set<String> = emptySet()
)

/**
 * Events from UI to ViewModel — unchanged from Chapter 6 except
 * [Refresh] and [Retry] are now handled by calling [LazyPagingItems.refresh]
 * directly in the composable rather than going through the ViewModel.
 */
sealed interface ArticleListEvent {
    data class SelectCategory(val category: Category?) : ArticleListEvent
    data class ToggleFavorite(val articleId: String, val isFavorite: Boolean) : ArticleListEvent
    data class ArticleClicked(val articleId: String) : ArticleListEvent
}