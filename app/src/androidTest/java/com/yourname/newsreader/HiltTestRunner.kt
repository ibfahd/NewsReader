package com.yourname.newsreader

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom instrumented test runner that swaps the Application class with
 * [HiltTestApplication] during tests.
 *
 * ─── Why is this needed? ──────────────────────────────────────────────────────
 * Hilt generates component code tied to [NewsReaderApplication]. For instrumented
 * tests, we need a Hilt-aware Application that:
 *   1. Doesn't clash with test-specific @HiltAndroidTest component generation.
 *   2. Resets Hilt's dependency graph between test classes.
 *   3. Accepts @UninstallModules and @BindValue test overrides.
 *
 * [HiltTestApplication] handles all of this. We just need to tell the test
 * framework to use it instead of our app's Application.
 *
 * Registration in app/build.gradle.kts:
 *   testInstrumentationRunner = "com.yourname.newsreader.HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}