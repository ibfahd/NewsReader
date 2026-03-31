package com.yourname.newsreader.ui.screens.articlelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import com.yourname.newsreader.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ch.7: The ViewModel now exposes [articlePagingFlow] instead of managing
 * article list state manually.
 *
 * ─── Key architectural changes ────────────────────────────────────────────────
 *
 * Before (Ch.6):
 *   - ViewModel managed Loading/Success/Error states via MutableStateFlow.
 *   - Collected a Flow<List<Article>> from Room and exposed it as uiState.
 *   - Had a refresh() function that called repository.refreshArticles().
 *
 * After (Ch.7):
 *   - ViewModel exposes a Flow<PagingData<Article>>.
 *   - Loading/Error/End states are managed by Paging 3's loadState.
 *   - The UI calls lazyPagingItems.refresh() directly — no ViewModel refresh needed.
 *   - uiState is now just the small non-paging UI concerns (category, favorites).
 *
 * ─── cachedIn(viewModelScope) ─────────────────────────────────────────────────
 * This is critical. Without it, every recomposition that collects the paging
 * flow would create a new Pager and throw away all loaded pages. [cachedIn]
 * keeps the most recent [PagingData] alive in the ViewModel's scope, so the
 * loaded pages survive recompositions, configuration changes, and navigation.
 *
 * ─── flatMapLatest ────────────────────────────────────────────────────────────
 * When the user changes the category filter, [selectedCategoryFlow] emits a new
 * value. [flatMapLatest] cancels the current paging flow and starts a new one
 * for the new category. The result is a fresh [PagingData] — no leftover articles
 * from the previous category.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_SELECTED_CATEGORY = "selected_category"
    }

    // ─── Internal state ───────────────────────────────────────────────────────

    private val selectedCategoryFlow = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SELECTED_CATEGORY)
            ?.let { Category.fromDisplayName(it) }
    )

    private val favoriteIdsFlow = repository.getFavoriteIds()

    // ─── Public state ─────────────────────────────────────────────────────────

    /**
     * The paged article stream — the primary data source for the article list.
     *
     * [flatMapLatest] ensures that when [selectedCategoryFlow] changes, the old
     * paging flow is cancelled and a new one starts immediately.
     *
     * [cachedIn] ensures pages survive configuration changes and recompositions.
     */
    val articlePagingFlow: Flow<PagingData<Article>> = selectedCategoryFlow
        .flatMapLatest { category -> repository.getPagedArticles(category) }
        .cachedIn(viewModelScope)

    /**
     * The non-paging UI state: which category is selected and which articles
     * are favorited. This is a [StateFlow] so the UI always has an initial value.
     */
    val uiState: StateFlow<ArticleListUiState> = selectedCategoryFlow.flatMapLatest { category ->
        favoriteIdsFlow.map { favoriteIds ->
            ArticleListUiState(
                selectedCategory = category,
                favoriteIds = favoriteIds
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ArticleListUiState()
    )

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    // ─── Event handling ───────────────────────────────────────────────────────

    fun onEvent(event: ArticleListEvent) {
        when (event) {
            is ArticleListEvent.SelectCategory -> selectCategory(event.category)
            is ArticleListEvent.ToggleFavorite -> toggleFavorite(event.articleId, event.isFavorite)
            is ArticleListEvent.ArticleClicked -> navigateToDetail(event.articleId)
        }
    }

    private fun selectCategory(category: Category?) {
        selectedCategoryFlow.update { category }
        savedStateHandle[KEY_SELECTED_CATEGORY] = category?.displayName
        // No manual reload needed — flatMapLatest reacts to the StateFlow change.
    }

    private fun toggleFavorite(articleId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(articleId, isFavorite) }
        }
    }

    private fun navigateToDetail(articleId: String) {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToDetail(articleId))
        }
    }
}

sealed interface NavigationEvent {
    data class NavigateToDetail(val articleId: String) : NavigationEvent
}