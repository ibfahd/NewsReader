package com.yourname.newsreader.ui.screens.articledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yourname.newsreader.data.repository.NewsRepository

/**
 * Factory for creating ArticleDetailViewModel.
 *
 * Required because ViewModel has constructor parameters.
 * In later chapters, this will be replaced with Hilt.
 */
class ArticleDetailViewModelFactory(
    private val repository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleDetailViewModel::class.java)) {
            return ArticleDetailViewModel(repository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
