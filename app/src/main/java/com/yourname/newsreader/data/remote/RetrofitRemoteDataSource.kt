package com.yourname.newsreader.data.remote

import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import com.yourname.newsreader.data.remote.dto.toEntity
import javax.inject.Inject
import com.yourname.newsreader.data.local.toDomain

/**
 * Production implementation of [RemoteDataSource] using Retrofit.
 *
 * This class replaces [MockRemoteDataSource] from Chapter 6.
 * The swap required changing one line in [RepositoryModule] — exactly as
 * designed. Every other class that depended on the [RemoteDataSource] interface
 * was completely unaffected.
 *
 * This implementation is used for the non-paged single-shot refresh:
 *   - The detail screen refresh
 *   - The repository's [refreshArticles] method
 *
 * For paginated loading, [ArticleRemoteMediator] calls [NewsApiService] directly
 * because it needs to pass page numbers — something [RemoteDataSource] doesn't
 * model. This is an intentional separation: the interface handles simple fetches,
 * the mediator handles paged fetches.
 *
 * @Inject constructor — Hilt resolves [NewsApiService] from [NetworkModule].
 */
class RetrofitRemoteDataSource @Inject constructor(
    private val apiService: NewsApiService
) : RemoteDataSource {

    /**
     * Fetches the first page of top headlines.
     *
     * Uses [safeApiCall] to wrap the response in [NetworkResult], then converts
     * it to domain objects or throws a meaningful exception on failure.
     *
     * @throws Exception on HTTP error or network failure — the caller (repository)
     *   catches this and decides whether to surface it to the UI.
     */
    override suspend fun fetchArticles(): List<Article> {
        return when (val result = safeApiCall { apiService.getTopHeadlines(page = 1) }) {
            is NetworkResult.Success -> {
                result.data.articles
                    ?.mapNotNull { dto ->
                        // toEntity maps DTO → Entity; toDomain maps Entity → Article.
                        // We go through the entity to keep the mapping logic centralised.
                        dto.toEntity(category = null)?.let { entity ->
                            toDomain(entity)
                        }
                    }
                    ?: emptyList()
            }
            is NetworkResult.HttpError -> throw Exception(
                "API error ${result.code}: ${result.message}"
            )
            is NetworkResult.NetworkError -> throw result.throwable
        }
    }
}

// ─── Private helper to bridge the ArticleEntity.toDomain extension ─────────
// The toDomain function is defined as an extension on ArticleEntity in
// ArticleEntity.kt. We need to call it without importing the entity in the
// repository. This private helper keeps the call isolated here.
private fun toDomain(entity: com.yourname.newsreader.data.local.ArticleEntity) =
    entity.toDomain()