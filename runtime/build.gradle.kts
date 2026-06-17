plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "io.github.wax911.runtime"
}

dependencies {
    implementation(project(":api"))
    implementation(project(":android-assets"))
    implementation(project(":annotations"))
}
