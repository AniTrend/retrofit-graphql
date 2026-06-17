plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.runtime"
}

dependencies {
    implementation(project(":api"))
    implementation(project(":android-assets"))
    implementation(project(":annotations"))
}
