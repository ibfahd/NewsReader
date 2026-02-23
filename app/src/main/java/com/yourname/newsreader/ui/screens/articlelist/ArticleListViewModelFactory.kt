package com.yourname.newsreader.ui.screens.articlelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yourname.newsreader.data.repository.NewsRepository

/**
 * Factory for creating ArticleListViewModel.
 *
 * Required because ViewModel has constructor parameters.
 * In later chapters, this will be replaced with Hilt.
 */
class ArticleListViewModelFactory(
    private val repository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleListViewModel::class.java)) {
            return ArticleListViewModel(repository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
