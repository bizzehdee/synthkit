package com.bizzeh.synthkit.testing

/**
 * Marks tests that a person runs by name, such as listening tests. The Gradle
 * test runs exclude it (see app/build.gradle.kts).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ManualOnly
