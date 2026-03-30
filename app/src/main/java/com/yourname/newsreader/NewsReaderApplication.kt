package com.yourname.newsreader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — the entry point for Hilt's component hierarchy.
 *
 * ─── What @HiltAndroidApp does ────────────────────────────────────────────────
 * At compile time, Hilt's KSP processor generates a concrete subclass:
 *   Hilt_NewsReaderApplication extends NewsReaderApplication
 *
 * At runtime, Android instantiates this Application class before any Activity,
 * which triggers Hilt's SingletonComponent initialisation. From this point,
 * all @Singleton-scoped dependencies are constructed and cached for the
 * entire lifetime of the process.
 *
 * ─── Manifest requirement ─────────────────────────────────────────────────────
 * AndroidManifest.xml must declare:
 *   android:name=".NewsReaderApplication"
 * Without this, Android instantiates the base Application class instead and
 * Hilt never initialises — every @Inject site would crash at runtime.
 */
@HiltAndroidApp
class NewsReaderApplication : Application()