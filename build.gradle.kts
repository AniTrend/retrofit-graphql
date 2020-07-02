buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(co.anitrend.retrofit.graphql.buildSrc.Libraries.Android.Tools.buildGradle)
        classpath(co.anitrend.retrofit.graphql.buildSrc.Libraries.JetBrains.Kotlin.Gradle.plugin)
        classpath(co.anitrend.retrofit.graphql.buildSrc.Libraries.JetBrains.Kotlin.Gradle.serialization)
        classpath(co.anitrend.retrofit.graphql.buildSrc.Libraries.Koin.Gradle.plugin)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks {
    val clean by registering(Delete::class) {
        delete(rootProject.buildDir)
    }
}