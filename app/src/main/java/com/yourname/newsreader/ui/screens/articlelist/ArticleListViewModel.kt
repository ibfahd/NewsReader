package com.yourname.newsreader.ui.screens.articlelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.newsreader.data.model.Category
import com.yourname.newsreader.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Article List screen — updated for Hilt (Chapter 6).
 *
 * ─── Changes from Chapter 5 ───────────────────────────────────────────────────
 * 1. Added @HiltViewModel + @Inject — enables Hilt auto-injection + factory generation.
 * 2. Deleted companion factory class (ArticleListViewModelFactory.kt deleted).
 * 3. Added initialRefresh() in init — implements offline-first startup.
 *
 * ─── @HiltViewModel ───────────────────────────────────────────────────────────
 * Hilt generates a ViewModelComponent (scoped to this ViewModel's lifecycle).
 * When combined with hiltViewModel() in the NavGraph, this ensures:
 *   - ViewModel survives configuration changes (standard Android behaviour).
 *   - [SavedStateHandle] is automatically populated with navigation arguments.
 *   - All constructor params are resolved from the Hilt dependency graph.
 *
 * NOTE: Unit tests can still instantiate this ViewModel directly:
 *   ArticleListViewModel(fakeRepository, SavedStateHandle())
 * The @HiltViewModel annotation does NOT prevent direct construction.
 *
 * ─── Offline-first startup ────────────────────────────────────────────────────
 * init block launches two coroutines concurrently:
 *   A. loadArticles()    → starts collecting from Room (instant, works offline)
 *   B. initialRefresh()  → fetches from network, writes to Room (triggers A)
 */
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_SELECTED_CATEGORY = "selected_category"
    }

    private val _uiState = MutableStateFlow<ArticleListState>(ArticleListState.Loading)
    val uiState: StateFlow<ArticleListState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    private var selectedCategory: Category? = savedStateHandle
        .get<String>(KEY_SELECTED_CATEGORY)
        ?.let { Category.fromDisplayName(it) }
        set(value) {
            field = value
            savedStateHandle[KEY_SELECTED_CATEGORY] = value?.displayName
        }

    init {
        loadArticles()       // A: start observing Room reactively
        initialRefresh()     // B: populate Room from network (triggers A's re-emission)
    }

    fun onEvent(event: ArticleListEvent) {
        when (event) {
            is ArticleListEvent.Refresh -> refresh()
            is ArticleListEvent.SelectCategory -> selectCategory(event.category)
            is ArticleListEvent.ToggleFavorite -> toggleFavorite(event.articleId, event.isFavorite)
            is ArticleListEvent.ArticleClicked -> navigateToDetail(event.articleId)
            is ArticleListEvent.Retry -> loadArticles()
        }
    }

    /**
     * Starts a long-running coroutine that collects Room's reactive Flow.
     * Room re-emits whenever refreshArticles() writes new data, driving UI updates.
     */
    private fun loadArticles() {
        viewModelScope.launch {
            _uiState.value = ArticleListState.Loading
            try {
                combine(
                    repository.getArticles(selectedCategory),
                    repository.getFavoriteIds()
                ) { articles, favoriteIds ->
                    ArticleListState.Success(
                        articles = articles,
                        selectedCategory = selectedCategory,
                        favoriteIds = favoriteIds,
                        isRefreshing = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = ArticleListState.Error(
                    message = "Failed to load articles: ${e.message}",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Background refresh on first launch — silently syncs Room with the network.
     * Failure is intentionally swallowed: the UI already shows cached Room data
     * (or an empty state), and the user can pull-to-refresh if needed.
     */
    private fun initialRefresh() {
        viewModelScope.launch {
            runCatching { repository.refreshArticles() }
            // Failure is logged in production; never shown as an error here
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { if (it is ArticleListState.Success) it.copy(isRefreshing = true) else it }
            try {
                repository.refreshArticles()
            } catch (e: Exception) {
                _uiState.value = ArticleListState.Error(
                    message = "Failed to refresh: ${e.message}",
                    canRetry = true
                )
            } finally {
                _uiState.update { if (it is ArticleListState.Success) it.copy(isRefreshing = false) else it }
            }
        }
    }

    private fun selectCategory(category: Category?) {
        selectedCategory = category
        loadArticles()
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