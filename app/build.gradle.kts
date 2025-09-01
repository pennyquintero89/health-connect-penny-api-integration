import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import java.util.Properties

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("kotlinx-serialization")
}

android {
    namespace = "com.healthsync.background"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.healthsync.background"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"${localProperties.getProperty("BASE_URL")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("androidx.health.connect:connect-client:1.2.0-alpha01")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("androidx.appcompat:appcompat:1.7.1")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")

    implementation("com.google.dagger:hilt-android:2.57.1")
    implementation("androidx.hilt:hilt-work:1.2.0")

    ksp("com.google.dagger:hilt-compiler:2.57.1")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    implementation(libs.androidx.junit.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test"))
}
