package com.yourname.newsreader.data.repository

import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for news articles.
 * 
 * This abstraction allows us to:
 * - Use mock data in this chapter
 * - Swap in real API implementation later
 * - Easily test ViewModels with test doubles
 * 
 * In Chapter 6, we'll create a real implementation with Hilt DI.
 */
interface NewsRepository {
    /**
     * Get all articles as a Flow for reactive updates.
     * 
     * @param category Optional category filter
     * @return Flow of article lists
     */
    fun getArticles(category: Category? = null): Flow<List<Article>>
    
    /**
     * Get a single article by ID.
     * 
     * @param articleId The article ID
     * @return The article, or null if not found
     */
    suspend fun getArticleById(articleId: String): Article?
    
    /**
     * Refresh articles from the source.
     * Simulates a network refresh.
     * 
     * @throws Exception if refresh fails
     */
    suspend fun refreshArticles()
    
    /**
     * Toggle favorite status for an article.
     * 
     * @param articleId The article ID
     * @param isFavorite New favorite status
     */
    suspend fun toggleFavorite(articleId: String, isFavorite: Boolean)
    
    /**
     * Get favorite article IDs.
     * 
     * @return Flow of favorite article IDs
     */
    fun getFavoriteIds(): Flow<Set<String>>
}
