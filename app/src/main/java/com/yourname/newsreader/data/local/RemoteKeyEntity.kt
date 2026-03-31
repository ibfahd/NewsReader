package com.yourname.newsreader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity that tracks pagination state for each article loaded via
 * [ArticleRemoteMediator].
 *
 * ─── Why remote keys are needed ───────────────────────────────────────────────
 * When the Paging library asks the [ArticleRemoteMediator] to load the next page,
 * it provides the current [PagingState] — a snapshot of what's currently in
 * Room. The mediator needs to translate "what's the last article I have?" into
 * "what page number should I request next?". That translation is the job of this
 * table.
 *
 * Each row records the prev/next page numbers for the article it is keyed on.
 * When the mediator needs to append, it finds the remote key for the last item
 * in the paged list and reads [nextKey].
 *
 * ─── The category field ───────────────────────────────────────────────────────
 * Because we page separately for each category (Technology, Business, etc.),
 * keys from one category must not bleed into another. The [category] column
 * allows [RemoteKeyDao.deleteRemoteKeysForCategory] to clear keys precisely
 * when the user switches categories or triggers a refresh.
 */
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    /** The article URL (same as [ArticleEntity.id]) that this key belongs to. */
    @PrimaryKey val articleId: String,
    /** The category filter that was active when this article was fetched. Null = all. */
    val category: String?,
    /** The page number before the current batch, or null if this is the first page. */
    val prevKey: Int?,
    /** The page number after the current batch, or null if this is the last page. */
    val nextKey: Int?
)