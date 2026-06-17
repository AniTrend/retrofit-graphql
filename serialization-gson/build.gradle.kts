plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "io.github.wax911.serialization.gson"
}

dependencies {
    implementation(project(":api"))
    implementation(libs.square.retrofit.gson.converter)
}
