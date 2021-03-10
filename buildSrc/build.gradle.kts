import java.net.URI

plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    google()
    jcenter()
    mavenCentral()
    maven {
        url = URI("https://www.jitpack.io")
    }
}

val buildToolsVersion = "4.1.2"
val kotlinVersion = "1.4.31"
val dokkaVersion = "0.10.1"
val manesVersion = "0.33.0"

dependencies {
    /* Depend on the android gradle plugin, since we want to access it in our plugin */
    implementation("com.android.tools.build:gradle:$buildToolsVersion")

    /* Depend on the kotlin plugin, since we want to access it in our plugin */
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    /* Depend on the dokka plugin, since we want to access it in our plugin */
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

    /** Dependency management */
    implementation("com.github.ben-manes:gradle-versions-plugin:$manesVersion")

    /* Depend on the default Gradle API's since we want to build a custom plugin */
    implementation(gradleApi())
    implementation(localGroovy())
}