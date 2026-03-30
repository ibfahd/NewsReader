package com.yourname.newsreader.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ─── DataStore instance ────────────────────────────────────────────────────────
// The preferencesDataStore delegate MUST be declared at file scope (top-level),
// NOT inside a class. This guarantees one DataStore instance per process,
// preventing the "multiple DataStore instances for the same file" crash.
//
// Internally it's a lazy property backed by a SingleProcessDataStore,
// which uses a coroutine-safe write queue to prevent concurrent write corruption.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"  // → files/datastore/user_preferences.preferences_pb
)

/**
 * Typed key-value store for user preferences, backed by Jetpack DataStore.
 *
 * ─── Why DataStore instead of SharedPreferences? ──────────────────────────────
 *
 *   SharedPreferences problems        DataStore solutions
 *   ─────────────────────────────     ──────────────────────────────────────────
 *   Blocking I/O on main thread   →   Fully async via coroutines (never blocks)
 *   No type-safety (getString/Int) →  Typed Preferences.Key<T> (compile-time safe)
 *   apply() can lose data on crash →  Atomic writes via atomic file replacement
 *   No reactive updates           →  Exposes Flow<Preferences> — live updates
 *
 * ─── What we store here ───────────────────────────────────────────────────────
 * Favourites (Set<String>) and the last-refresh timestamp (Long).
 * Articles themselves live in Room — better querying, relational structure.
 *
 * ─── Hilt scoping ─────────────────────────────────────────────────────────────
 * @Singleton — one instance for the whole app. Hilt constructs it automatically
 * because of the @Inject constructor (no DatabaseModule entry needed).
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Access the top-level DataStore extension property via our injected context.
    private val dataStore = context.dataStore

    // ─── Typed Keys ──────────────────────────────────────────────────────────
    // Keys are objects with an explicit type parameter — the compiler prevents
    // you from writing a String to a Long key. The string names are persisted
    // in the file: once shipped, never rename them (old data becomes unreachable).
    private object Keys {
        val FAVORITE_IDS          = stringSetPreferencesKey("favorite_ids")
        val LAST_REFRESH_TIMESTAMP = longPreferencesKey("last_refresh_timestamp")
    }

    // ─── Favourites ──────────────────────────────────────────────────────────

    /**
     * Observe favourite article IDs as a reactive [Flow].
     * Every [toggleFavorite] call triggers a new emission to all collectors.
     *
     * .map { } transforms the full Preferences snapshot into just the Set we need.
     * This is more efficient than collecting the whole snapshot in the ViewModel.
     */
    fun getFavoriteIds(): Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_IDS] ?: emptySet()
    }

    /**
     * Toggle the favourite state of a single article.
     *
     * dataStore.edit { } is an atomic coroutine transaction:
     *   1. Reads a MutablePreferences snapshot
     *   2. Applies our lambda
     *   3. Writes the result atomically (crash during write → no partial state)
     *
     * Set arithmetic (+/-) on immutable Sets produces a new Set without mutation.
     */
    suspend fun toggleFavorite(articleId: String, isFavorite: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_IDS] ?: emptySet()
            prefs[Keys.FAVORITE_IDS] = if (isFavorite) current + articleId
            else current - articleId
        }
    }

    // ─── Refresh timestamp ────────────────────────────────────────────────────

    /**
     * The epoch-millis time of the last successful network refresh.
     * Defaults to 0 (never refreshed).
     * Can be used to implement "refresh if stale for > N hours" logic.
     */
    fun getLastRefreshTimestamp(): Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_REFRESH_TIMESTAMP] ?: 0L
    }

    /** Record that a refresh just succeeded. */
    suspend fun updateLastRefreshTimestamp() {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_REFRESH_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    // ─── Migration note ───────────────────────────────────────────────────────
    // Migrating FROM SharedPreferences? DataStore has a built-in migration API:
    //
    //   preferencesDataStore(
    //       name = "user_preferences",
    //       produceMigrations = { context ->
    //           listOf(SharedPreferencesMigration(context, "old_prefs_name"))
    //       }
    //   )
    //
    // DataStore reads the SharedPreferences file once, copies the data,
    // then deletes the old file. Transparent to callers.
}