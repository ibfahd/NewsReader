package com.yourname.newsreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for [RemoteKeyEntity] — the pagination bookkeeping table.
 *
 * These methods are called exclusively from [ArticleRemoteMediator], always
 * inside a [withTransaction] block to ensure remote keys and article rows
 * are written atomically. If the transaction fails midway (e.g. low memory),
 * neither the keys nor the articles are partially written.
 */
@Dao
interface RemoteKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<RemoteKeyEntity>)

    @Query("SELECT * FROM remote_keys WHERE articleId = :articleId LIMIT 1")
    suspend fun getRemoteKeyByArticleId(articleId: String): RemoteKeyEntity?

    /**
     * Delete keys for a specific category on refresh.
     * [category] IS NULL OR category = :category mirrors the same nullable
     * pattern used in [ArticleDao.getArticles].
     */
    @Query("DELETE FROM remote_keys WHERE (:category IS NULL AND category IS NULL) OR category = :category")
    suspend fun deleteRemoteKeysForCategory(category: String?)

    @Query("DELETE FROM remote_keys")
    suspend fun clearAll()
}