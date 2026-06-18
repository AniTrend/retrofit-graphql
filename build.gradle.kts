buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.jetbrains.kotlin.gradle)
        classpath(libs.jetbrains.kotlin.serialization)
        classpath(libs.symbol.processing)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
