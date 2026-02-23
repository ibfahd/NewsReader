package com.yourname.newsreader.ui.screens.articlelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.newsreader.data.model.Category
import com.yourname.newsreader.data.repository.NewsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Article List screen - IMPROVED VERSION
 * 
 * Key improvements:
 * - Proper refresh state management
 * - Clear separation of state and events
 * - Testable architecture
 * - SavedStateHandle integration
 * 
 * This demonstrates best practices for ViewModel + StateFlow in Compose.
 */
class ArticleListViewModel(
    private val repository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val KEY_SELECTED_CATEGORY = "selected_category"
    }
    
    // Internal mutable state
    private val _uiState = MutableStateFlow<ArticleListState>(ArticleListState.Loading)
    val uiState: StateFlow<ArticleListState> = _uiState.asStateFlow()
    
    // One-time navigation events
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()
    
    // Selected category (restored from SavedStateHandle)
    private var selectedCategory: Category? = savedStateHandle
        .get<String>(KEY_SELECTED_CATEGORY)
        ?.let { Category.fromDisplayName(it) }
        set(value) {
            field = value
            savedStateHandle[KEY_SELECTED_CATEGORY] = value?.displayName
        }
    
    init {
        // Load initial data
        loadArticles()
    }
    
    /**
     * Handle UI events.
     */
    fun onEvent(event: ArticleListEvent) {
        when (event) {
            is ArticleListEvent.Refresh -> refresh()
            is ArticleListEvent.SelectCategory -> selectCategory(event.category)
            is ArticleListEvent.ToggleFavorite -> toggleFavorite(
                event.articleId,
                event.isFavorite
            )
            is ArticleListEvent.ArticleClicked -> navigateToDetail(event.articleId)
            is ArticleListEvent.Retry -> loadArticles()
        }
    }
    
    /**
     * Load articles from repository.
     * 
     * Combines articles and favorites, applies category filter.
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
     * Refresh articles (pull-to-refresh).
     */
    private fun refresh() {
        viewModelScope.launch {
            // Show refresh indicator
            _uiState.update { currentState ->
                if (currentState is ArticleListState.Success) {
                    currentState.copy(isRefreshing = true)
                } else {
                    currentState
                }
            }
            
            try {
                // Refresh from repository
                repository.refreshArticles()
                
                // Data will update automatically via Flow
            } catch (e: Exception) {
                _uiState.value = ArticleListState.Error(
                    message = "Failed to refresh: ${e.message}",
                    canRetry = true
                )
            } finally {
                // Hide refresh indicator
                _uiState.update { currentState ->
                    if (currentState is ArticleListState.Success) {
                        currentState.copy(isRefreshing = false)
                    } else {
                        currentState
                    }
                }
            }
        }
    }
    
    /**
     * Select category filter.
     */
    private fun selectCategory(category: Category?) {
        selectedCategory = category
        loadArticles()
    }
    
    /**
     * Toggle article favorite status.
     */
    private fun toggleFavorite(articleId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(articleId, isFavorite)
            } catch (e: Exception) {
                // In production, show error to user
                println("Failed to toggle favorite: ${e.message}")
            }
        }
    }
    
    /**
     * Navigate to article detail.
     */
    private fun navigateToDetail(articleId: String) {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToDetail(articleId))
        }
    }
}

/**
 * One-time navigation events.
 */
sealed interface NavigationEvent {
    data class NavigateToDetail(val articleId: String) : NavigationEvent
}
