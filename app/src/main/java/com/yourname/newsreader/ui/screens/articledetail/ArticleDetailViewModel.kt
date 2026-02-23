package com.yourname.newsreader.ui.screens.articledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.newsreader.data.repository.NewsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Article Detail screen.
 * 
 * Demonstrates:
 * - Receiving navigation arguments via SavedStateHandle
 * - Combining multiple Flow sources
 * - Handling user actions
 * - One-time side effects (share, open browser)
 */
class ArticleDetailViewModel(
    private val repository: NewsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        const val ARG_ARTICLE_ID = "articleId"
    }
    
    // Get article ID from navigation arguments
    private val articleId: String = checkNotNull(
        savedStateHandle.get<String>(ARG_ARTICLE_ID)
    ) {
        "Article ID is required"
    }
    
    // Internal mutable state
    private val _uiState = MutableStateFlow<ArticleDetailState>(
        ArticleDetailState.Loading
    )
    val uiState: StateFlow<ArticleDetailState> = _uiState.asStateFlow()
    
    // One-time side effects
    private val _sideEffects = MutableSharedFlow<SideEffect>()
    val sideEffects = _sideEffects.asSharedFlow()
    
    init {
        loadArticle()
    }
    
    /**
     * Handle UI events.
     */
    fun onEvent(event: ArticleDetailEvent) {
        when (event) {
            is ArticleDetailEvent.ToggleFavorite -> toggleFavorite()
            is ArticleDetailEvent.NavigateBack -> navigateBack()
            is ArticleDetailEvent.ShareArticle -> shareArticle()
            is ArticleDetailEvent.OpenInBrowser -> openInBrowser()
        }
    }
    
    /**
     * Load article and favorite status.
     */
    private fun loadArticle() {
        viewModelScope.launch {
            _uiState.value = ArticleDetailState.Loading
            
            try {
                // Get article
                val article = repository.getArticleById(articleId)
                
                if (article == null) {
                    _uiState.value = ArticleDetailState.Error(
                        message = "Article not found"
                    )
                    return@launch
                }
                
                // Combine article with favorite status
                repository.getFavoriteIds().collect { favoriteIds ->
                    _uiState.value = ArticleDetailState.Success(
                        article = article,
                        isFavorite = article.id in favoriteIds
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ArticleDetailState.Error(
                    message = "Failed to load article: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Toggle favorite status.
     */
    private fun toggleFavorite() {
        val currentState = _uiState.value
        if (currentState !is ArticleDetailState.Success) return
        
        viewModelScope.launch {
            try {
                val newFavoriteStatus = !currentState.isFavorite
                repository.toggleFavorite(articleId, newFavoriteStatus)
                
                // Update state optimistically
                _uiState.update { state ->
                    if (state is ArticleDetailState.Success) {
                        state.copy(isFavorite = newFavoriteStatus)
                    } else {
                        state
                    }
                }
            } catch (e: Exception) {
                // In production, show error to user
                println("Failed to toggle favorite: ${e.message}")
            }
        }
    }
    
    /**
     * Navigate back.
     */
    private fun navigateBack() {
        viewModelScope.launch {
            _sideEffects.emit(SideEffect.NavigateBack)
        }
    }
    
    /**
     * Share article.
     */
    private fun shareArticle() {
        val currentState = _uiState.value
        if (currentState !is ArticleDetailState.Success) return
        
        viewModelScope.launch {
            _sideEffects.emit(
                SideEffect.ShareArticle(
                    title = currentState.article.title,
                    url = currentState.article.url
                )
            )
        }
    }
    
    /**
     * Open article in browser.
     */
    private fun openInBrowser() {
        val currentState = _uiState.value
        if (currentState !is ArticleDetailState.Success) return
        
        viewModelScope.launch {
            _sideEffects.emit(
                SideEffect.OpenUrl(currentState.article.url)
            )
        }
    }
}

/**
 * One-time side effects for the detail screen.
 * 
 * These represent actions that should happen once,
 * not state that should be observed continuously.
 */
sealed interface SideEffect {
    data object NavigateBack : SideEffect
    data class ShareArticle(val title: String, val url: String) : SideEffect
    data class OpenUrl(val url: String) : SideEffect
}
