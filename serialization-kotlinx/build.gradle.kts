plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.serialization.kotlinx"
}

dependencies {
    // GraphQLJson interface is exposed in public API
    api(project(":api"))
    implementation(libs.jetbrains.kotlinx.serialization.json)
}
