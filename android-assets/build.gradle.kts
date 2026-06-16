plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "io.github.wax911.androidassets"
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.androidx.annotation)
}
