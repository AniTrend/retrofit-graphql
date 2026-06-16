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

    testImplementation(libs.jetbrains.kotlin.reflect)
}
