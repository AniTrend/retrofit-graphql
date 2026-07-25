plugins {
    id("co.anitrend.retrofit.graphql")
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
}

android {
    namespace = "co.anitrend.retrofit.graphql.api"
}

dependencies {
    // @Serializable annotation and runtime types for kotlinx.serialization support
    implementation(libs.jetbrains.kotlinx.serialization.core)
}
