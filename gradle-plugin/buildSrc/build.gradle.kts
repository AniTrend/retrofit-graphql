plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.8.0")
    implementation(gradleApi())
}
