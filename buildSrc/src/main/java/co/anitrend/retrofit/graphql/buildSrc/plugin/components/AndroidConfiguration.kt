package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isSampleModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libraryExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.spotlessExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.baseAppExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.kotlinAndroidProjectExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libs
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.props
import org.gradle.api.JavaVersion
import org.gradle.api.Project

import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask


private fun Project.configureLint() = libraryExtension().run {
    lint {
        abortOnError = false
        ignoreWarnings = false
        ignoreTestSources = true
    }
}

internal fun Project.configureSpotless() {
    if (!isSampleModule())
        spotlessExtension().run {
            kotlin {
                target("**/*.kt")
                targetExclude(
                    "${layout.buildDirectory.get()}/**/*.kt",
                    "**/androidTest/**/*.kt",
                    "**/test/**/*.kt",
                    "bin/**/*.kt"
                )
                ktlint(libs.pintrest.ktlint.get().version)
                licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
            }
        }
}

internal fun Project.configureAndroid() {
    if (isSampleModule())
        baseAppExtension().run {
            compileSdkVersion(37)
            defaultConfig {
                minSdk = 23
                targetSdk = 37
                versionCode = props[PropertyTypes.CODE].toInt()
                versionName = props[PropertyTypes.VERSION]
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                applicationId = "co.anitrend.retrofit.graphql.sample"
                vectorDrawables.useSupportLibrary = true
            }
            buildFeatures {
                viewBinding = true
            }

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = false
                    isTestCoverageEnabled = false
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                }

                getByName("debug") {
                    isMinifyEnabled = false
                    isTestCoverageEnabled = true
                }
            }

            packagingOptions {
                resources.excludes.add("META-INF/NOTICE.*")
                resources.excludes.add("META-INF/LICENSE*")
            }

            sourceSets {
                map { androidSourceSet ->
                    androidSourceSet.java.srcDir(
                        "src/${androidSourceSet.name}/kotlin"
                    )
                }
            }

            testOptions {
                unitTests.isIncludeAndroidResources = true
                unitTests.isReturnDefaultValues = true
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    else
        libraryExtension().run {
            compileSdkVersion(37)
            defaultConfig {
                minSdk = 23
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            lint {
                abortOnError = false
                ignoreWarnings = false
                ignoreTestSources = true
            }

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = false
                    isTestCoverageEnabled = false
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                }

                getByName("debug") {
                    isMinifyEnabled = false
                    isTestCoverageEnabled = true
                }
            }

            packagingOptions {
                resources.excludes.add("META-INF/NOTICE.*")
                resources.excludes.add("META-INF/LICENSE*")
            }

            sourceSets {
                map { androidSourceSet ->
                    androidSourceSet.java.srcDir(
                        "src/${androidSourceSet.name}/kotlin"
                    )
                }
                if (!project.isSampleModule()) {
                    getByName("test") {
                        resources.srcDirs(file("src/test/resources"))
                    }
                }
            }

            testOptions {
                unitTests.isIncludeAndroidResources = true
                unitTests.isReturnDefaultValues = true
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }

    tasks.withType(KotlinCompilationTask::class.java) {
        compilerOptions {
            allWarningsAsErrors.set(false)
            // Filter out modules that won't be using coroutines
            if (isSampleModule()) {
                freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
    }

    // Note: useJUnitPlatform() removed - requires junit-platform-launcher on test runtime classpath.
    // All tests are currently JUnit 4; re-enable when JUnit 5 migration happens with platform deps.

    kotlinAndroidProjectExtension().run {
        jvmToolchain(21)
    }
}