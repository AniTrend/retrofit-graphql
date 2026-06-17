plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.serialization.kotlinx"
}

dependencies {
    implementation(project(":api"))
    implementation(libs.jetbrains.kotlinx.serialization.json)
}
