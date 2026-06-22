plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.serialization.kotlinx"
}

dependencies {
    // GraphQLJson interface is exposed in public API
    api(project(":api"))
    // kotlinx.serialization.json.Json is exposed in KotlinxGraphQLJson public constructor
    api(libs.jetbrains.kotlinx.serialization.json)
}
