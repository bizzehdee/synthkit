plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bizzeh.synthkit"
    compileSdk = 37
    compileSdkMinor = 2
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        applicationId = "com.bizzeh.synthkit"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Listening tests play audio for a person; they run only when started by name.
        testInstrumentationRunnerArguments["notAnnotation"] = "com.bizzeh.synthkit.testing.ManualOnly"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                // Oboe's prefab package is built against the shared C++ runtime.
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        prefab = true
        // The latency readout is shown in debug builds only.
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Pin the compiler rather than inheriting the daemon's JVM, which may be a JRE
// with no compiler or a Java version AGP does not support.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.oboe)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// The platform-independent C++ audio code is tested on the build host with
// GoogleTest, because device-side native tests would need a device for every run.
val nativeHostTestDir = layout.buildDirectory.dir("native-host-test")
val nativeHostTestSource = layout.projectDirectory.dir("src/test/cpp")

val configureNativeHostTest by tasks.registering(Exec::class) {
    inputs.dir(nativeHostTestSource)
    commandLine(
        "cmake", "-S", nativeHostTestSource.asFile.absolutePath,
        "-B", nativeHostTestDir.get().asFile.absolutePath, "-G", "Ninja",
        // Clang ships its sanitizer runtime; GCC needs separate libasan packages.
        "-DCMAKE_CXX_COMPILER=clang++",
    )
}

val buildNativeHostTest by tasks.registering(Exec::class) {
    dependsOn(configureNativeHostTest)
    commandLine("cmake", "--build", nativeHostTestDir.get().asFile.absolutePath)
}

val nativeHostTest by tasks.registering(Exec::class) {
    group = "verification"
    description = "C++ audio engine tests on the build host."
    dependsOn(buildNativeHostTest)
    commandLine(
        "ctest", "--test-dir", nativeHostTestDir.get().asFile.absolutePath, "--output-on-failure", "--no-tests=error",
    )
}

tasks.register("verify") {
    group = "verification"
    description = "Unit tests, native host tests and lint. No device required."
    dependsOn("testDebugUnitTest", nativeHostTest, "lintDebug")
}
