import co.anitrend.retrofit.graphql.codegen.config.SerializationBackend
import org.gradle.api.GradleException
import java.net.URI

plugins {
    id("co.anitrend.retrofit.graphql")
    id("co.anitrend.retrofit.graphql.codegen")
    id("kotlinx-serialization")
}

repositories {
    maven {
        url = URI("https://jitpack.io")
    }
}

android {
    namespace = "co.anitrend.retrofit.graphql.sample"
    defaultConfig {
        buildConfigField("String", "token", "\"\"")
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    testBuildType = "release"
    testOptions {
        managedDevices {
            localDevices {
                create("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
    lint {
        abortOnError = false
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(project(":runtime"))
    implementation(project(":api"))
    implementation(project(":serialization-kotlinx"))

    implementation(libs.jetbrains.kotlinx.serialization.json)

    implementation(libs.google.material)
    implementation(libs.threeTenBp)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recycler.view)

    implementation(libs.androidx.lifecycle.livedata.core)
    implementation(libs.androidx.lifecycle.livedata.core.ktx)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.runtime.ktx)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.anitrend.arch.ui)
    implementation(libs.anitrend.arch.extension)
    implementation(libs.anitrend.arch.analytics)
    implementation(libs.anitrend.arch.core)
    implementation(libs.anitrend.arch.data)
    implementation(libs.anitrend.arch.theme)
    implementation(libs.anitrend.arch.domain)
    implementation(libs.anitrend.arch.recycler)
    implementation(libs.anitrend.arch.request)
    implementation(libs.anitrend.arch.paging.legacy)
    implementation(libs.anitrend.arch.recycler.paging.legacy)

    implementation(libs.anitrend.emojify)
    implementation(libs.anitrend.emojify.contract)
    implementation(libs.anitrend.emojify.kotlinx)

    implementation(libs.coil)
    implementation(libs.coil.gif)

    implementation(libs.square.retrofit)
    implementation(libs.square.okhttp)
    implementation(libs.square.okhttp.logger)


    implementation(libs.timber)

    releaseImplementation(libs.chuncker.release)
    debugImplementation(libs.chuncker.debug)

    testImplementation(libs.jetbrains.kotlinx.coroutines.test)

    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation(libs.jetbrains.kotlinx.serialization.json)
    androidTestImplementation(libs.jetbrains.kotlinx.coroutines.test)
}

retrofitGraphQL {
    common {
        generateVariables.set(true)
        generateResponses.set(true)
        serializationBackend.set(SerializationBackend.KOTLINX)
    }
    packageName.set("co.anitrend.retrofit.graphql.sample.generated")
    schema.set(file("src/main/graphql/schema.graphql"))
    operations.from(fileTree("src/main/graphql") {
        include("**/*.graphql")
        exclude("**/bucket/**")
    })
    scalars {
        map("DateTime", "kotlin.String")
        map("GitObjectID", "kotlin.String")
        map("URI", "kotlin.String")
        map("Upload", "kotlin.String")
    }
}

tasks.register("verifyReleaseMapping") {
    group = "verification"
    description = "Verifies release R8 mapping for generated DTOs and Gson upload keep rules."
    dependsOn("assembleRelease")

    val mappingFile = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
    inputs.file(mappingFile)

    doLast {
        val file = mappingFile.get().asFile
        if (!file.isFile) {
            throw GradleException("Release mapping file not found: ${file.absolutePath}")
        }

        val lines = file.readLines()
        val classMappings = lines
            .filter { it.isNotBlank() && !it.startsWith(" ") && !it.startsWith("#") }
            .associate { line ->
                val originalName = line.substringBefore(" -> ")
                val mappedName = line.substringAfter(" -> ").removeSuffix(":")
                originalName to mappedName
            }

        fun mappedNameFor(className: String): String = classMappings[className]
            ?: throw GradleException("Missing release mapping entry for $className")

        fun assertRenamed(className: String) {
            val mappedName = mappedNameFor(className)
            if (mappedName == className || mappedName.substringAfterLast('.') == className.substringAfterLast('.')) {
                throw GradleException("Expected $className to be renamed in release mapping, but found $mappedName")
            }
        }

        fun assertNotRenamed(className: String) {
            val mappedName = mappedNameFor(className)
            if (mappedName != className) {
                throw GradleException("Expected $className to be kept for Gson reflection, but found $mappedName")
            }
        }

        fun classBlock(className: String): List<String> {
            val start = lines.indexOfFirst { it == "$className -> ${mappedNameFor(className)}:" }
            if (start == -1) {
                throw GradleException("Missing release mapping block for $className")
            }
            val end = (start + 1 until lines.size)
                .firstOrNull { index ->
                    lines[index].isNotBlank() && !lines[index].startsWith(" ") && !lines[index].startsWith("#")
                }
                ?: lines.size
            return lines.subList(start + 1, end).map { it.trim() }
        }

        fun assertFieldNotRenamedIfPresent(className: String, fieldName: String) {
            val memberLine = classBlock(className).firstOrNull { member ->
                member.substringBefore(" -> ").substringAfterLast(' ') == fieldName
            }
            if (memberLine == null) {
                logger.lifecycle("No member mapping entry for $className.$fieldName; treating field as not renamed by mapping.")
                return
            }
            val mappedName = memberLine.substringAfter(" -> ")
            if (mappedName != fieldName) {
                throw GradleException("Expected $className.$fieldName to be kept for Gson reflection, but found $mappedName")
            }
        }

        val generatedPackage = "co.anitrend.retrofit.graphql.sample.generated"
        assertRenamed("$generatedPackage.GetCurrentUserData")
        assertRenamed("$generatedPackage.GetMarketPlaceAppsData")
        val unionResponseClass = "$generatedPackage.GeneralSearchData"
        assertRenamed(unionResponseClass)
        assertRenamed("$unionResponseClass\$SearchEdgesNode")
        assertRenamed("$unionResponseClass\$SearchEdgesNode\$Repository")

        val graphQLRequest = "co.anitrend.retrofit.graphql.model.GraphQLRequest"
        assertNotRenamed(graphQLRequest)
        assertFieldNotRenamedIfPresent(graphQLRequest, "query")
        assertFieldNotRenamedIfPresent(graphQLRequest, "operationName")
        assertFieldNotRenamedIfPresent(graphQLRequest, "variables")

        val uploadVariables = "co.anitrend.retrofit.graphql.sample.bucket.UploadToStorageBucketVariables"
        assertNotRenamed(uploadVariables)
        assertFieldNotRenamedIfPresent(uploadVariables, "upload")
    }
}

tasks.register("releaseR8Verification") {
    group = "verification"
    description = "Runs release R8 mapping checks and managed-device release instrumentation tests."
    dependsOn("verifyReleaseMapping", "pixel2api30ReleaseAndroidTest")
}
