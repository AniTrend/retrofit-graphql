plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "io.github.wax911.library"
}

dependencies {
    api(project(":annotations"))
    api(project(":api"))
    api(project(":android-assets"))
    api(project(":runtime"))
    api(project(":serialization-gson"))
    api(project(":serialization-kotlinx"))

    testImplementation(libs.jetbrains.kotlin.reflect)
}
