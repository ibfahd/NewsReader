package com.yourname.newsreader.data.model

import java.util.Date

/**
 * Domain model representing a news article.
 * 
 * This is a simple data class that will be used throughout the app.
 * In later chapters, we'll map API responses to this model.
 */
data class Article(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val author: String,
    val publishedAt: Date,
    val imageUrl: String?,
    val category: Category,
    val source: String,
    val url: String
)
