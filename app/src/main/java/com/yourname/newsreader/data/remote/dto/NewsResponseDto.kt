package com.yourname.newsreader.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * The top-level API response envelope from NewsAPI.
 *
 * Example JSON:
 * {
 *   "status": "ok",
 *   "totalResults": 38,
 *   "articles": [ { ... }, { ... } ]
 * }
 *
 * ─── Safe parsing ─────────────────────────────────────────────────────────────
 * All fields are nullable or have defaults, which prevents a [JsonDataException]
 * if the API ever omits a field or changes the response structure. Moshi will
 * assign null/default rather than crashing. The repository handles null cases
 * by substituting sensible fallbacks (e.g. empty list for null articles).
 *
 * ─── totalResults ─────────────────────────────────────────────────────────────
 * This tells us how many results exist in total across all pages. Paging 3's
 * RemoteMediator uses it indirectly — once [articles] is empty, we know we've
 * reached the end rather than needing to compare against totalResults, which
 * is simpler and works even if the API count is inaccurate.
 */
@JsonClass(generateAdapter = true)
data class NewsResponseDto(
    val status: String?,
    val totalResults: Int?,
    val articles: List<ArticleDto>?
)