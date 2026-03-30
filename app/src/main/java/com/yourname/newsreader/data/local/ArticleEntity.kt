package com.yourname.newsreader.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import java.util.Date

/**
 * Room Entity — one instance maps to one row in the "articles" table.
 *
 * ─── Entity vs Domain model ───────────────────────────────────────────────────
 * We deliberately keep two separate classes:
 *
 *   ArticleEntity  (this file)   — persistence concern, lives in data/local
 *   Article        (domain model)— business concern, lives in data/model
 *
 * Why the separation?
 *   - The UI and ViewModels never import Room annotations.
 *   - We can evolve the schema (add columns, rename) without touching the UI.
 *   - Mapping is explicit and testable (see toDomain() / toEntity() below).
 *
 * ─── Type decisions ───────────────────────────────────────────────────────────
 * publishedAt: Long (epoch millis) — NOT Date with a TypeConverter.
 *   • Long is a native SQLite type → zero conversion cost on every read.
 *   • ORDER BY publishedAt works correctly (Long comparisons are exact).
 *   • TypeConverters add a function call per row; with thousands of articles
 *     that overhead matters. See Converters.kt for when TypeConverters ARE needed.
 *
 * category: String (Category.name) — NOT the ordinal Int.
 *   • Storing as "TECHNOLOGY" is human-readable in DB inspection tools.
 *   • Ordinals break if you ever reorder the enum; names are stable.
 *
 * ─── Indices ──────────────────────────────────────────────────────────────────
 * Two indices are added for the two most common query patterns:
 *   1. WHERE category = ?     → Index(["category"])
 *   2. ORDER BY publishedAt   → Index(["publishedAt"])
 * Trade-off: slightly larger DB file and slower writes, but dramatically faster
 * reads. For a read-heavy news app, this is always the right call.
 */
@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["category"]),
        Index(value = ["publishedAt"])
    ]
)
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val content: String,
    val author: String,
    /** Stored as epoch milliseconds — avoids TypeConverter, natively sortable. */
    val publishedAt: Long,
    val imageUrl: String?,
    /** Stored as Category.name (e.g. "TECHNOLOGY") for stability and readability. */
    val category: String,
    val source: String,
    val url: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Mapping extension functions
// Placed here (not in a separate Mapper class) because they're tightly coupled
// to the entity structure. If the entity changes, the mapper changes too.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converts a Room [ArticleEntity] to the domain [Article].
 * Called in the Repository when data leaves the data layer.
 */
fun ArticleEntity.toDomain(): Article = Article(
    id = id,
    title = title,
    description = description,
    content = content,
    author = author,
    publishedAt = Date(publishedAt),         // Long → java.util.Date
    imageUrl = imageUrl,
    category = Category.valueOf(category),    // String → enum (safe: we always write .name)
    source = source,
    url = url
)

/**
 * Converts a domain [Article] to a [ArticleEntity] ready for Room persistence.
 * Called in the Repository when data enters the data layer (after a network fetch).
 */
fun Article.toEntity(): ArticleEntity = ArticleEntity(
    id = id,
    title = title,
    description = description,
    content = content,
    author = author,
    publishedAt = publishedAt.time,          // java.util.Date → epoch millis
    imageUrl = imageUrl,
    category = category.name,                // enum → String
    source = source,
    url = url
)