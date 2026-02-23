package com.yourname.newsreader.ui.screens.articledetail

import com.yourname.newsreader.data.model.Article

/**
 * UI state for the Article Detail screen.
 * 
 * Similar pattern to ArticleListState but simpler:
 * - Loading: Fetching article
 * - Success: Article loaded
 * - Error: Article not found or error
 */
sealed interface ArticleDetailState {
    /**
     * Loading article data.
     */
    data object Loading : ArticleDetailState
    
    /**
     * Article loaded successfully.
     * 
     * @param article The article to display
     * @param isFavorite Whether the article is favorited
     */
    data class Success(
        val article: Article,
        val isFavorite: Boolean
    ) : ArticleDetailState
    
    /**
     * Failed to load article.
     * 
     * @param message Error message
     */
    data class Error(
        val message: String
    ) : ArticleDetailState
}

/**
 * Events from UI to ViewModel.
 */
sealed interface ArticleDetailEvent {
    /**
     * User toggled favorite status.
     */
    data object ToggleFavorite : ArticleDetailEvent
    
    /**
     * User clicked back button.
     */
    data object NavigateBack : ArticleDetailEvent
    
    /**
     * User wants to share the article.
     */
    data object ShareArticle : ArticleDetailEvent
    
    /**
     * User wants to open article in browser.
     */
    data object OpenInBrowser : ArticleDetailEvent
}
