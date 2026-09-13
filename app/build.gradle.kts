/*
 * Copyright 2025 Narra Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.api.variant.HostTestBuilder
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.time.Duration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.paparazzi)
    id("narra.spotless")
    jacoco
}

// Release secrets may come from the environment (CI) or local.properties (local signing).
// They are never committed; see docs/RELEASING.md.
val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

fun releaseProperty(key: String): String? =
    System.getenv(key)?.takeIf { it.isNotBlank() } ?: localProperties.getProperty(key)?.takeIf { it.isNotBlank() }

val releaseKeystorePath = releaseProperty("RELEASE_KEYSTORE_FILE")
val configuredVersionCode = releaseProperty("VERSION_CODE")?.toIntOrNull()
val configuredVersionName = releaseProperty("VERSION_NAME")

android {
    namespace = "com.mienaiknife.narra"
    compileSdk = 36

    // Forced re-sync to fix IDE indexing
    defaultConfig {
        applicationId = "com.mienaiknife.narra"
        minSdk = 24
        targetSdk = 36
        versionCode = configuredVersionCode ?: 1
        versionName = configuredVersionName ?: "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystorePath != null) {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseProperty("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = releaseProperty("RELEASE_KEY_ALIAS")
                keyPassword = releaseProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "2048m"
            it.maxParallelForks = 1
            // Kill runaway tests instead of hanging the build indefinitely.
            it.timeout.set(Duration.ofMinutes(10))

            if (project.hasProperty("skipPaparazzi")) {
                (it as Test).exclude("**/screenshots/**")
            }
        }
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    packaging {
        resources {
            // Merge (do not drop) license notices shipped by dependencies; Epublib is LGPL-3.0
            // and its notice must be preserved in the distributed APK.
            merges += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

// AGP only creates unit-test tasks for the default build type. Opt the release variant in so the
// suite is exercised against the release configuration (see docs/RELEASING.md).
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}

tasks.register<Exec>("clearAppData") {
    group = "verification"
    description = "Clears the app data using adb."

    // Use the adb executable from the SDK if possible, fallback to "adb" in PATH
    val adb =
        try {
            val extension = project.extensions.getByName("android") as com.android.build.gradle.BaseExtension
            extension.adbExecutable.absolutePath
        } catch (_: Exception) {
            "adb"
        }

    doFirst {
        println("Stopping and clearing data for com.mienaiknife.narra...")
    }

    commandLine(adb, "shell", "am force-stop com.mienaiknife.narra; pm clear com.mienaiknife.narra")

    // Ignore exit value in case no device is connected or app is not installed
    isIgnoreExitValue = true

    standardOutput = System.out
    errorOutput = System.err
}

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates code coverage reports for the debug unit tests."

    dependsOn("testDebugUnitTest")

    val excludes =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*_Hilt*.class",
            "**/Hilt_*.class",
            "**/*_Impl*.class",
            "**/*_Factory*.class",
            "**/*_MembersInjector*.class",
            "**/di/**",
            "**/data/local/entities/**",
            "**/*Database_Impl*",
            "**/*Dao_Impl*",
            "**/*Directions*",
        )

    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("intermediates/classes/debug/transformDebugClassesWithAsm/dirs")) { exclude(excludes) },
            fileTree(layout.buildDirectory.dir("intermediates/classes/debug/hiltJavaCompileDebug")) { exclude(excludes) },
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { exclude(excludes) },
            fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) { exclude(excludes) },
        ),
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.jsoup)
    implementation(libs.readability4j)
    implementation(libs.rssparser)
    implementation(libs.epublib) {
        exclude(group = "xmlpull", module = "xmlpull")
        exclude(group = "net.sf.kxml", module = "kxml2")
    }
    implementation(libs.sherpa.onnx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.compose.foundation)
    ksp(libs.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.media3.session)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.coil.svg)
    implementation(libs.commons.compress)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.glance.appwidget.preview)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestUtil(libs.androidx.test.services)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register("checkSherpaUpdate") {
    group = "help"
    description = "Checks if there is a newer version of Sherpa-ONNX available on GitHub."
    doLast {
        val currentVersion = libs.versions.sherpaOnnx.get()
        val latestReleaseUrl = "https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/latest"
        try {
            val url = uri(latestReleaseUrl).toURL()
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                // Simple regex to extract tag_name from GitHub API response
                val match = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(response)
                val latestVersion = match?.groupValues?.get(1)?.removePrefix("v") ?: "unknown"

                println("\n--- Sherpa-ONNX Version Check ---")
                println("Current version: $currentVersion")
                println("Latest version:  $latestVersion")

                if (currentVersion != latestVersion) {
                    println("\n[!] A newer version of Sherpa-ONNX is available!")
                    println("Check releases here: https://github.com/k2-fsa/sherpa-onnx/releases")
                } else {
                    println("\n[✓] Sherpa-ONNX is up to date.")
                }
                println("----------------------------------\n")
            } else {
                println("Failed to check for updates: HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            println("Error checking for Sherpa-ONNX updates: ${e.message}")
        }
    }
}
