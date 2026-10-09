plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 provides Kotlin support itself; only the Compose compiler plugin is
    // applied separately. https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.kotlin.compose) apply false
}
