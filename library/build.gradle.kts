plugins {
    id("co.anitrend.retrofit.graphql")
    id("kotlin-parcelize")
}

android {
    namespace = "io.github.wax911.library"
}

dependencies {
    implementation(libs.androidx.annotation)

    testImplementation(libs.jetbrains.kotlin.reflect)
}