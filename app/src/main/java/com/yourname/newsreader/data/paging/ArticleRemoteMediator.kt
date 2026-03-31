package com.yourname.newsreader.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.yourname.newsreader.data.local.ArticleEntity
import com.yourname.newsreader.data.local.NewsDatabase
import com.yourname.newsreader.data.local.RemoteKeyEntity
import com.yourname.newsreader.data.model.Category
import com.yourname.newsreader.data.remote.NewsApiService
import com.yourname.newsreader.data.remote.dto.toEntity
import retrofit2.HttpException
import java.io.IOException

/**
 * The bridge between the network and Room for Paging 3.
 *
 * ─── What a RemoteMediator does ───────────────────────────────────────────────
 * With Paging 3, the [PagingSource] reads data from Room (the local database),
 * NOT from the network. This gives instant, offline-capable loading. The
 * RemoteMediator's job is to keep Room populated with fresh network data:
 *
 *   Paging library                RemoteMediator            NewsAPI
 *        │                              │                      │
 *        │──"I need more data"──────────▶│                      │
 *        │                              │──GET /v2/headlines──▶│
 *        │                              │◀────response─────────│
 *        │                              │──INSERT into Room    │
 *        │◀──Flow<PagingData> updates──(Room emits)            │
 *
 * ─── The three LoadTypes ──────────────────────────────────────────────────────
 * The Paging library calls [load] with one of three types:
 *
 *   REFRESH  — Initial load, or user triggered a pull-to-refresh.
 *              Strategy: fetch page 1, delete old data for this category,
 *              replace with fresh data.
 *
 *   PREPEND  — The user has scrolled to the top and we might want to load
 *              content before the current first item. NewsAPI is chronological
 *              only, so we never prepend — signal end-of-pagination immediately.
 *
 *   APPEND   — The user has scrolled near the bottom. Load the next page of
 *              network data and append it to Room.
 *
 * ─── Remote keys ──────────────────────────────────────────────────────────────
 * The RemoteMediator receives a [PagingState] snapshot of Room's current content.
 * It needs to answer: "given the articles I currently have, what page should I
 * request next?" The [RemoteKeyEntity] table provides this answer. Each article
 * has an associated key row that records which page it came from and what the
 * next/previous pages are.
 *
 * ─── Atomic transactions ──────────────────────────────────────────────────────
 * All database writes happen inside [withTransaction]. If the process is killed
 * mid-write, neither the articles nor the remote keys will be partially saved.
 * This prevents a state where some articles have keys but others don't.
 */
@OptIn(ExperimentalPagingApi::class)
class ArticleRemoteMediator(
    private val category: Category?,
    private val apiService: NewsApiService,
    private val db: NewsDatabase
) : RemoteMediator<Int, ArticleEntity>() {

    private val articleDao = db.articleDao()
    private val remoteKeyDao = db.remoteKeyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ArticleEntity>
    ): MediatorResult {

        // Step 1: Determine which page to fetch.
        val page: Int = when (loadType) {
            LoadType.REFRESH -> {
                // On refresh, find the remote key closest to the current scroll
                // position. If we have one, go back one page to ensure overlap.
                // If we have none (first ever load), start from page 1.
                val remoteKey = getRemoteKeyClosestToCurrentPosition(state)
                remoteKey?.nextKey?.minus(1) ?: STARTING_PAGE
            }
            LoadType.PREPEND -> {
                // Find the key for the first item currently in the list.
                val remoteKey = getRemoteKeyForFirstItem(state)
                // If prevKey is null, we're already at the beginning — signal
                // end of pagination so the library stops trying to prepend.
                remoteKey?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
            }
            LoadType.APPEND -> {
                // Find the key for the last item currently in the list.
                val remoteKey = getRemoteKeyForLastItem(state)
                // If nextKey is null, we've reached the end of the dataset.
                remoteKey?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
            }
        }

        // Step 2: Fetch from network.
        return try {
            val response = apiService.getTopHeadlines(
                category = category?.name?.lowercase(),
                page = page,
                pageSize = state.config.pageSize
            )
            val dtos = response.articles ?: emptyList()
            val endOfPaginationReached = dtos.isEmpty()

            // Step 3: Write to Room in a single atomic transaction.
            db.withTransaction {
                // On refresh: clear old data for this category so we don't
                // serve a mix of old and new pages after the user refreshes.
                if (loadType == LoadType.REFRESH) {
                    remoteKeyDao.deleteRemoteKeysForCategory(category?.name)
                    articleDao.deleteArticlesForCategory(category?.name)
                }

                // Compute the page numbers for each article's key row.
                val prevKey = if (page == STARTING_PAGE) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1

                // Convert DTOs to entities, filtering out removed/malformed articles.
                val entities = dtos.mapNotNull { dto -> dto.toEntity(category) }

                // Each article gets a matching remote key row for future pagination.
                val keys = entities.map { entity ->
                    RemoteKeyEntity(
                        articleId = entity.id,
                        category = category?.name,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                }

                remoteKeyDao.insertAll(keys)
                articleDao.insertArticles(entities)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)

        } catch (e: IOException) {
            // No network, DNS failure, timeout — transient, user can retry.
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            // Server returned 4xx or 5xx. Could be an expired API key (401)
            // or rate limiting (429). Surface to the UI via loadState.
            MediatorResult.Error(e)
        }
    }

    // ─── Remote key helper functions ─────────────────────────────────────────
    // These three functions translate "a position in the PagingState" into
    // "the remote key for the article at that position".

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, ArticleEntity>
    ): RemoteKeyEntity? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestItemToPosition(anchorPosition)?.id?.let { id ->
                remoteKeyDao.getRemoteKeyByArticleId(id)
            }
        }
    }

    private suspend fun getRemoteKeyForFirstItem(
        state: PagingState<Int, ArticleEntity>
    ): RemoteKeyEntity? {
        return state.pages
            .firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { remoteKeyDao.getRemoteKeyByArticleId(it.id) }
    }

    private suspend fun getRemoteKeyForLastItem(
        state: PagingState<Int, ArticleEntity>
    ): RemoteKeyEntity? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { remoteKeyDao.getRemoteKeyByArticleId(it.id) }
    }

    companion object {
        const val STARTING_PAGE = 1
    }
}