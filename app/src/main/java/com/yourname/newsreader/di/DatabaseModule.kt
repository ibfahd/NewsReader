package com.yourname.newsreader.di

import android.content.Context
import androidx.room.Room
import com.yourname.newsreader.data.local.ArticleDao
import com.yourname.newsreader.data.local.NewsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module: provides Room database and DAO instances.
 *
 * ─── Anatomy of a Hilt Module ────────────────────────────────────────────────
 *
 * @Module           — this class teaches Hilt how to create things it
 *                     can't construct automatically (builders, third-party
 *                     objects, interfaces). Pure-Kotlin classes with
 *                     @Inject constructors don't need a Module.
 *
 * @InstallIn(SingletonComponent::class)
 *                   — scopes these bindings to the Application lifetime.
 *                     Other components:
 *                       ActivityComponent    — per-Activity
 *                       ViewModelComponent   — per-ViewModel
 *                       FragmentComponent    — per-Fragment
 *                     Use the narrowest scope that makes sense.
 *
 * @Provides         — annotates functions that return a dependency. Hilt
 *                     calls the function and caches the result (if @Singleton).
 *
 * @Singleton        — on a @Provides function: "call this once, reuse forever."
 *                     Without it, Hilt creates a new instance per injection.
 *
 * object vs abstract class:
 *   - Use `object` (no instances needed) when all functions are @Provides.
 *   - Use `abstract class` when any function is @Binds (this Module uses both,
 *     so the @Binds live in RepositoryModule instead).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Builds the Room database — called exactly once by Hilt.
     *
     * [fallbackToDestructiveMigration]: if the schema version changes without
     * a matching Migration, Room wipes and recreates the DB.
     * ⚠️ NEVER in production — always write proper Migrations instead.
     * Remove this line and add .addMigrations(NewsDatabase.MIGRATION_X_Y).
     */
    @Provides
    @Singleton
    fun provideNewsDatabase(
        @ApplicationContext context: Context
    ): NewsDatabase = Room.databaseBuilder(
        context,
        NewsDatabase::class.java,
        NewsDatabase.DATABASE_NAME
    )
        .fallbackToDestructiveMigration(dropAllTables = true) // DEV ONLY
        // .addMigrations(NewsDatabase.MIGRATION_1_2)         // uncomment for production
        .build()

    /**
     * Extracts the DAO from the database.
     *
     * Hilt can inject [NewsDatabase] here because [provideNewsDatabase] just
     * taught it how to create one. This is the dependency graph in action:
     * declare your pieces, Hilt wires them together in the right order.
     */
    @Provides
    @Singleton
    fun provideArticleDao(database: NewsDatabase): ArticleDao = database.articleDao()
}