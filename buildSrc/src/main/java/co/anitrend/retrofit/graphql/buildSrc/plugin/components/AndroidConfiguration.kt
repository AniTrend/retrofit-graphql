package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.baseExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isSampleModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libraryExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.spotlessExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.baseAppExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.kotlinAndroidProjectExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libs
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.props
import com.android.build.gradle.internal.dsl.DefaultConfig
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import java.io.File


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

private fun DefaultConfig.applyAdditionalConfiguration(project: Project) {
    if (project.isSampleModule()) {
        applicationId = "co.anitrend.retrofit.graphql.sample"
        project.baseAppExtension().buildFeatures {
            viewBinding = true
        }
        project.logger.lifecycle("Applying vector drawables configuration for module -> ${project.path}")
        vectorDrawables.useSupportLibrary = true
    }
    else {
        project.configureLint()
        consumerProguardFiles.add(File("consumer-rules.pro"))
    }
}

internal fun Project.configureAndroid(): Unit = baseExtension().run {
    compileSdkVersion(34)
    defaultConfig {
        minSdk = if (isSampleModule()) 21 else 17
        targetSdk = 34
        versionCode = props[PropertyTypes.CODE].toInt()
        versionName = props[PropertyTypes.VERSION]
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applyAdditionalConfiguration(project)
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType(KotlinCompile::class.java) {
        kotlinOptions {
            allWarningsAsErrors = false
            kotlinOptions {
                allWarningsAsErrors = false
                // Filter out modules that won't be using coroutines
                freeCompilerArgs = if (isSampleModule()) listOf(
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                ) else emptyList()
            }
        }
    }

    kotlinAndroidProjectExtension().run {
        jvmToolchain(17)
    }
}