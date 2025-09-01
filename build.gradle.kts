// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
}

// build.gradle.kts (Project level)
buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.57.1")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.2.10")
    }
}