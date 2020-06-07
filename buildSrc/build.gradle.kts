plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    google()
    jcenter()
    mavenCentral()
}

val kotlinVersion = "1.3.72"
val buildToolsVersion = "4.0.0"
val dokkaVersion = "0.10.1"

dependencies {
    /* Depend on the android gradle plugin, since we want to access it in our plugin */
    implementation("com.android.tools.build:gradle:$buildToolsVersion")

    /* Depend on the kotlin plugin, since we want to access it in our plugin */
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    /* Depend on the dokka plugin, since we want to access it in our plugin */
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

    /* Depend on the default Gradle API's since we want to build a custom plugin */
    implementation(gradleApi())
    implementation(localGroovy())
}