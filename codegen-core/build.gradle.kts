import co.anitrend.retrofit.graphql.buildSrc.plugin.components.configureSpotlessForJvm

plugins {
    kotlin("jvm")
}

configureSpotlessForJvm()

dependencies {
    implementation(libs.graphql.java)
    implementation(libs.kotlinpoet)

    testImplementation(libs.junit)
}

