package com.yourname.newsreader.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.yourname.newsreader.data.local.ArticleEntity
import com.yourname.newsreader.data.model.Category
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Data Transfer Object representing a single article from the NewsAPI.
 *
 * ─── DTO vs Domain model ──────────────────────────────────────────────────────
 * This class mirrors the JSON structure exactly — null fields, snake_case names,
 * ISO 8601 date strings. It is the API's contract, not ours. The domain model
 * [Article] is our contract — clean, non-null where possible, typed dates.
 *
 * The mapping from DTO → Entity happens in [toEntity], which lives here because
 * it is tightly coupled to the DTO structure. If the API changes a field name,
 * this is the only file that needs updating.
 *
 * ─── @JsonClass(generateAdapter = true) ──────────────────────────────────────
 * This annotation tells Moshi's KSP processor to generate a type-safe
 * [ArticleDtoJsonAdapter] at compile time. Without it, Moshi would fall back to
 * slow reflection. With it, parsing is as fast as handwritten code.
 *
 * ─── @Json(name = "...") ──────────────────────────────────────────────────────
 * Maps the JSON key name to a Kotlin property name. Without this annotation,
 * the JSON key and the property name must match exactly.
 */
@JsonClass(generateAdapter = true)
data class ArticleDto(
    val source: SourceDto?,
    val author: String?,
    val title: String?,
    val description: String?,
    val url: String,
    @Json(name = "urlToImage") val imageUrl: String?,
    val publishedAt: String?,
    val content: String?
)

@JsonClass(generateAdapter = true)
data class SourceDto(
    val id: String?,
    val name: String?
)

// ─────────────────────────────────────────────────────────────────────────────
// Mapping: DTO → Entity
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converts a [ArticleDto] to an [ArticleEntity] for Room persistence.
 *
 * Returns null if the article has been removed or is malformed — NewsAPI marks
 * removed articles with "[Removed]" in the title. Returning null allows the
 * caller to filter them out with `.mapNotNull { it.toEntity(category) }`.
 *
 * @param category The [Category] that was requested — the API doesn't embed
 *   category in article responses, so we tag each article with the filter
 *   that was used to retrieve it.
 */
fun ArticleDto.toEntity(category: Category?): ArticleEntity? {
    // Guard: filter out removed articles and articles without a usable title.
    val safeTitle = title?.takeIf { it.isNotBlank() && it != "[Removed]" } ?: return null

    return ArticleEntity(
        // Use the URL as the primary key. URLs are always present and globally
        // unique across articles. Numeric IDs like "1", "2" would collide across
        // different API calls.
        id = url,
        title = safeTitle,
        description = description?.takeIf { it.isNotBlank() } ?: "",
        content = content
            ?.substringBefore(" [+")   // NewsAPI truncates content with "[+N chars]"
            ?.takeIf { it.isNotBlank() } ?: "",
        author = author?.takeIf { it.isNotBlank() } ?: source?.name ?: "Unknown",
        publishedAt = parsePublishedAt(publishedAt),
        imageUrl = imageUrl,
        category = (category ?: Category.GENERAL).name,
        source = source?.name ?: "Unknown",
        url = url
    )
}

/**
 * Parses the ISO 8601 date string from the API (e.g. "2024-01-15T10:30:00Z")
 * into epoch milliseconds for Room storage.
 *
 * Falls back to [System.currentTimeMillis] if parsing fails — a missing date
 * should not prevent an article from being saved and displayed.
 */
private fun parsePublishedAt(dateString: String?): Long {
    if (dateString == null) return System.currentTimeMillis()
    return try {
        // The NewsAPI always returns UTC dates in ISO 8601 format.
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .parse(dateString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}