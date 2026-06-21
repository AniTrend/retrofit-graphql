plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.serialization.gson"
}

dependencies {
    // GraphQLJson interface is exposed in public API
    api(project(":api"))
    implementation(libs.square.retrofit.gson.converter)
}
