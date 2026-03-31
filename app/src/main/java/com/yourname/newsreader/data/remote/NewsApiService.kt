package com.yourname.newsreader.data.remote

import com.yourname.newsreader.data.remote.dto.NewsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for the NewsAPI v2.
 *
 * ─── How Retrofit works ───────────────────────────────────────────────────────
 * Retrofit reads this interface at startup and generates a concrete implementation
 * using dynamic proxies. Every annotated method becomes a fully implemented HTTP
 * call. You never write OkHttp request builders manually.
 *
 * ─── suspend functions ────────────────────────────────────────────────────────
 * Retrofit 2.6+ supports suspend functions natively. Under the hood, it uses a
 * coroutine-aware [CallAdapter] that suspends the calling coroutine while the
 * request is in flight, then resumes it on the calling dispatcher when the
 * response arrives. No callbacks, no rxJava, no blocking.
 *
 * ─── Why no API key here? ─────────────────────────────────────────────────────
 * The API key is injected by an OkHttp interceptor in [NetworkModule]. Putting it
 * here as a @Query parameter on every method would mean updating every method if
 * the auth mechanism changes. The interceptor is a single point of change.
 *
 * ─── About the free tier ──────────────────────────────────────────────────────
 * The NewsAPI free developer plan:
 *   - Only serves articles up to 1 month old.
 *   - Limits to 100 requests/day.
 *   - Returns a maximum of 100 results per query.
 *   - Requires attribution.
 * The paid tiers lift these restrictions. Our paging implementation handles
 * the 100-result ceiling gracefully via end-of-pagination detection.
 */
interface NewsApiService {

    /**
     * Fetch top headlines for a country/category combination.
     *
     * @param country   ISO 3166-1 alpha-2 country code. "us" is the default.
     * @param category  One of: business, entertainment, general, health,
     *                  science, sports, technology. Null returns all categories.
     * @param page      1-based page number for pagination.
     * @param pageSize  Number of articles per page (max 100 for the paid tier,
     *                  max 20 recommended for the free tier to conserve quota).
     */
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("country")  country:  String  = "us",
        @Query("category") category: String? = null,
        @Query("page")     page:     Int     = 1,
        @Query("pageSize") pageSize: Int     = PAGE_SIZE
    ): NewsResponseDto

    companion object {
        const val BASE_URL  = "https://newsapi.org/"
        const val PAGE_SIZE = 20
    }
}