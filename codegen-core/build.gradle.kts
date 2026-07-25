import co.anitrend.retrofit.graphql.buildSrc.plugin.components.configureDokkaForJvm
import co.anitrend.retrofit.graphql.buildSrc.plugin.components.configureJvmPublishing
import co.anitrend.retrofit.graphql.buildSrc.plugin.components.configureSpotlessForJvm

plugins {
    kotlin("jvm")
}

configureSpotlessForJvm()
configureDokkaForJvm()
configureJvmPublishing()

dependencies {
    implementation(libs.graphql.java)
    implementation(libs.kotlinpoet)

    testImplementation(libs.junit)
    testImplementation(libs.jetbrains.kotlinx.serialization.json)
    testImplementation(libs.gson)
    testImplementation(libs.jetbrains.kotlin.reflect)
    testImplementation(libs.jetbrains.kotlin.compiler.embeddable)
}

