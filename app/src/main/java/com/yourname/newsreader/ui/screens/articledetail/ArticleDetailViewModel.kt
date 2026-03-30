package com.yourname.newsreader.ui.screens.articledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.newsreader.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Article Detail screen — updated for Hilt (Chapter 6).
 *
 * ─── Changes from Chapter 5 ───────────────────────────────────────────────────
 * 1. Added @HiltViewModel + @Inject.
 * 2. Deleted ArticleDetailViewModelFactory.kt.
 * 3. [SavedStateHandle] is now provided automatically by Hilt + Navigation Compose.
 *    When hiltViewModel() creates this ViewModel for a NavBackStackEntry, Navigation
 *    puts all route arguments ("articleId") into the SavedStateHandle first.
 *    Result: savedStateHandle.get<String>("articleId") still works identically.
 *
 * All business logic is identical to Chapter 5 — Hilt only changes how the
 * ViewModel is constructed, not what it does.
 */
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val repository: NewsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val ARG_ARTICLE_ID = "articleId"
    }

    private val articleId: String = checkNotNull(savedStateHandle.get<String>(ARG_ARTICLE_ID)) {
        "ArticleDetailViewModel requires an 'articleId' navigation argument"
    }

    private val _uiState = MutableStateFlow<ArticleDetailState>(ArticleDetailState.Loading)
    val uiState: StateFlow<ArticleDetailState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<SideEffect>()
    val sideEffects = _sideEffects.asSharedFlow()

    init { loadArticle() }

    fun onEvent(event: ArticleDetailEvent) {
        when (event) {
            is ArticleDetailEvent.ToggleFavorite -> toggleFavorite()
            is ArticleDetailEvent.NavigateBack   -> navigateBack()
            is ArticleDetailEvent.ShareArticle   -> shareArticle()
            is ArticleDetailEvent.OpenInBrowser  -> openInBrowser()
        }
    }

    /**
     * Load the article from the repository's cache hierarchy (L1 memory → L2 Room),
     * then keep observing DataStore for favourite status changes.
     */
    private fun loadArticle() {
        viewModelScope.launch {
            _uiState.value = ArticleDetailState.Loading
            try {
                val article = repository.getArticleById(articleId)
                if (article == null) {
                    _uiState.value = ArticleDetailState.Error("Article not found")
                    return@launch
                }
                repository.getFavoriteIds().collect { favoriteIds ->
                    _uiState.value = ArticleDetailState.Success(
                        article = article,
                        isFavorite = article.id in favoriteIds
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ArticleDetailState.Error("Failed to load article: ${e.message}")
            }
        }
    }

    private fun toggleFavorite() {
        val current = _uiState.value as? ArticleDetailState.Success ?: return
        viewModelScope.launch {
            val newStatus = !current.isFavorite
            runCatching { repository.toggleFavorite(articleId, newStatus) }
            _uiState.update { state ->
                if (state is ArticleDetailState.Success) state.copy(isFavorite = newStatus) else state
            }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch { _sideEffects.emit(SideEffect.NavigateBack) }
    }

    private fun shareArticle() {
        val current = _uiState.value as? ArticleDetailState.Success ?: return
        viewModelScope.launch {
            _sideEffects.emit(SideEffect.ShareArticle(current.article.title, current.article.url))
        }
    }

    private fun openInBrowser() {
        val current = _uiState.value as? ArticleDetailState.Success ?: return
        viewModelScope.launch { _sideEffects.emit(SideEffect.OpenUrl(current.article.url)) }
    }
}

sealed interface SideEffect {
    data object NavigateBack : SideEffect
    data class ShareArticle(val title: String, val url: String) : SideEffect
    data class OpenUrl(val url: String) : SideEffect
}