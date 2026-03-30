package com.yourname.newsreader.data.local

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverters — teach Room how to persist types it doesn't natively support.
 *
 * ─── When are TypeConverters needed? ─────────────────────────────────────────
 * Room stores: Int, Long, Double, Float, String, ByteArray — that's it.
 * For anything else you have two strategies:
 *
 *   A) TypeConverter (this class) — serialize ↔ deserialize on every access.
 *   B) Flatten to a primitive directly (our choice for Date in ArticleEntity).
 *
 * Strategy B is preferred for performance-critical columns because it eliminates
 * the function-call overhead per row. Use TypeConverters for:
 *   - Types you can't control (third-party, complex objects)
 *   - Columns queried rarely (e.g., a blob of metadata)
 *   - List<String> serialised as JSON (when a join table is overkill)
 *
 * ─── Example: Date ↔ Long ─────────────────────────────────────────────────────
 * If ArticleEntity declared `val publishedAt: Date` instead of Long, Room would
 * fail at compile time without this converter. With it:
 *
 *   INSERT: Room calls dateToTimestamp(date) → stores the Long
 *   SELECT: Room calls fromTimestamp(long)   → gives you back a Date
 *
 * ─── Registration ─────────────────────────────────────────────────────────────
 * Converters must be registered on the @Database class:
 *   @TypeConverters(Converters::class)
 *   abstract class NewsDatabase : RoomDatabase()
 *
 * They then apply to ALL entities and DAOs in that database automatically.
 */
class Converters {

    /**
     * Database → Kotlin: convert a stored [Long] timestamp into a [Date].
     * Nullable: a null Long means a null Date (e.g. "unknown publish date").
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    /**
     * Kotlin → Database: convert a [Date] to a [Long] for storage.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}