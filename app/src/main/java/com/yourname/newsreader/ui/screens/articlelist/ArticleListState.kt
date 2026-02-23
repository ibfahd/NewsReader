package com.yourname.newsreader.ui.screens.articlelist

import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category

/**
 * UI state for the Article List screen.
 * 
 * This sealed interface represents all possible states:
 * - Loading: Initial load or refresh
 * - Success: Articles loaded successfully
 * - Error: Something went wrong
 * 
 * Using a sealed interface ensures exhaustive when statements
 * and makes it impossible to have invalid state combinations.
 */
sealed interface ArticleListState {
    /**
     * Loading state - shown during initial load or refresh
     */
    data object Loading : ArticleListState
    
    /**
     * Success state - articles loaded and ready to display
     * 
     * @param articles List of articles to display
     * @param selectedCategory Currently selected category filter
     * @param favoriteIds Set of favorite article IDs
     * @param isRefreshing Whether a refresh is in progress
     */
    data class Success(
        val articles: List<Article>,
        val selectedCategory: Category?,
        val favoriteIds: Set<String>,
        val isRefreshing: Boolean = false
    ) : ArticleListState
    
    /**
     * Error state - something went wrong
     * 
     * @param message Error message to display
     * @param canRetry Whether the user can retry the operation
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : ArticleListState
}

/**
 * Events that can be sent from the UI to the ViewModel.
 * 
 * This pattern ensures clear separation between UI and business logic.
 * The UI sends events, the ViewModel processes them and updates state.
 */
sealed interface ArticleListEvent {
    /**
     * User requested a refresh (pull-to-refresh)
     */
    data object Refresh : ArticleListEvent
    
    /**
     * User selected a category filter
     * 
     * @param category The selected category, or null for all
     */
    data class SelectCategory(val category: Category?) : ArticleListEvent
    
    /**
     * User toggled favorite status
     * 
     * @param articleId The article ID
     * @param isFavorite New favorite status
     */
    data class ToggleFavorite(
        val articleId: String,
        val isFavorite: Boolean
    ) : ArticleListEvent
    
    /**
     * User tapped on an article
     * 
     * @param articleId The article ID
     */
    data class ArticleClicked(val articleId: String) : ArticleListEvent
    
    /**
     * User requested to retry after an error
     */
    data object Retry : ArticleListEvent
}
