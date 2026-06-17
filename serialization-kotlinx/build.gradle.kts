plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "io.github.wax911.serialization.kotlinx"
}

dependencies {
    implementation(project(":api"))
    implementation(libs.jetbrains.kotlinx.serialization.json)
}
