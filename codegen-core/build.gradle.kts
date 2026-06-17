plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.graphql.java)
    implementation(libs.kotlinpoet)

    testImplementation(libs.junit)
}
