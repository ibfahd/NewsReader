package com.yourname.newsreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The Room database — single entry point for all local persistence.
 *
 * ─── Architecture ─────────────────────────────────────────────────────────────
 *   ViewModel → Repository → ArticleDao → NewsDatabase → SQLite file
 *
 * Room generates the concrete implementation (NewsDatabase_Impl) at compile
 * time, so this class stays abstract. The singleton instance is created once
 * in DatabaseModule and shared across the entire app via Hilt.
 *
 * ─── Schema versioning & migrations ──────────────────────────────────────────
 * [version] must be incremented every time the schema changes. Without a
 * matching Migration, existing users' data is inaccessible (or wiped if
 * fallbackToDestructiveMigration is enabled).
 *
 * Migration pattern (example: adding a "isBookmarked" column in v2):
 *
 *   val MIGRATION_1_2 = object : Migration(1, 2) {
 *       override fun migrate(db: SupportSQLiteDatabase) {
 *           db.execSQL(
 *               "ALTER TABLE articles ADD COLUMN isBookmarked INTEGER NOT NULL DEFAULT 0"
 *           )
 *       }
 *   }
 *
 * Then register it in DatabaseModule:
 *   .addMigrations(NewsDatabase.MIGRATION_1_2)
 *
 * ─── Schema export ────────────────────────────────────────────────────────────
 * [exportSchema = true] (configured via KSP arg "room.schemaLocation") writes
 * a JSON snapshot of the schema to /schemas/<version>.json. Commit these files
 * — they're your migration audit trail and can be used in MigrationTestHelper.
 */
@Database(
    entities = [ArticleEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NewsDatabase : RoomDatabase() {

    /** Room generates the concrete ArticleDao_Impl at compile time. */
    abstract fun articleDao(): ArticleDao

    companion object {
        const val DATABASE_NAME = "news_reader.db"

        // ── Example migrations (uncomment when bumping version) ────────────────
        //
        // val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE articles ADD COLUMN isBookmarked INTEGER NOT NULL DEFAULT 0")
        //     }
        // }
    }
}