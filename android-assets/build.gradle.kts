plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.androidassets"
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.androidx.annotation)
}
