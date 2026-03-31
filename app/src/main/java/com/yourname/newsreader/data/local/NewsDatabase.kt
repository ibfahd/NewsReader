package com.yourname.newsreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Ch.7 changes:
 *   - [version] bumped from 1 → 2.
 *   - [RemoteKeyEntity] added to [entities].
 *   - [remoteKeyDao] abstract function added.
 *   - [MIGRATION_1_2] defined: creates the "remote_keys" table.
 *     In [DatabaseModule], swap [fallbackToDestructiveMigration] for
 *     [.addMigrations(MIGRATION_1_2)] before shipping to production.
 */
@Database(
    entities = [ArticleEntity::class, RemoteKeyEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NewsDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun remoteKeyDao(): RemoteKeyDao

    companion object {
        const val DATABASE_NAME = "news_reader.db"

        /**
         * Migration from v1 → v2: add the remote_keys table.
         *
         * ALTER TABLE cannot create new tables — we use CREATE TABLE instead.
         * The column types match exactly what Room would generate for
         * [RemoteKeyEntity]: TEXT PRIMARY KEY, TEXT nullable, INTEGER nullable.
         *
         * To apply in production, replace [fallbackToDestructiveMigration] in
         * [DatabaseModule] with [.addMigrations(MIGRATION_1_2)].
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS remote_keys (
                        articleId TEXT NOT NULL PRIMARY KEY,
                        category TEXT,
                        prevKey INTEGER,
                        nextKey INTEGER
                    )
                    """.trimIndent()
                )
            }
        }
    }
}