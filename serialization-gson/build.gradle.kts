plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.serialization.gson"
}

dependencies {
    implementation(project(":api"))
    implementation(libs.square.retrofit.gson.converter)
}
