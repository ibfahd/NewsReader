package com.yourname.newsreader.data.model

/**
 * News article categories.
 * 
 * Used for filtering and organizing articles.
 */
enum class Category(val displayName: String) {
    TECHNOLOGY("Technology"),
    BUSINESS("Business"),
    SCIENCE("Science"),
    HEALTH("Health"),
    SPORTS("Sports"),
    ENTERTAINMENT("Entertainment"),
    GENERAL("General");
    
    companion object {
        fun fromDisplayName(name: String): Category? {
            return entries.find { it.displayName == name }
        }
    }
}
