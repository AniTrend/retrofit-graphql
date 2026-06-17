plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql"
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
