plugins {
    alias(libs.plugins.android.application) apply false
    // kotlin-android is REMOVED. AGP 9.0 bundles Kotlin support directly.
    // Applying kotlin.android on top of AGP 9.0 throws a fatal InvalidUserCodeException.
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.hilt.android)        apply false
    alias(libs.plugins.ksp)                 apply false
    alias(libs.plugins.room)                apply false
}