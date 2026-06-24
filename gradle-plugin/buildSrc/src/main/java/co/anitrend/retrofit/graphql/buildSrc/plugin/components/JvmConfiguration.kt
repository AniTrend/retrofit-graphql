package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import java.io.File
import java.util.Properties

/**
 * Minimal stub for the composite build. Configures spotless for JVM modules
 * using a project-relative copyright file when available.
 */
fun Project.configureSpotlessForJvm() {
    plugins.apply("com.diffplug.spotless")
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude(
                "${layout.buildDirectory.get()}/**/*.kt",
                "**/test/**/*.kt",
                "bin/**/*.kt",
            )
            ktlint("1.0.1")
                .editorConfigOverride(mapOf("ktlint_standard_filename" to "disabled"))
            val copyrightFile = rootProject.file("../../spotless/copyright.kt")
            if (copyrightFile.exists()) {
                licenseHeaderFile(copyrightFile)
            }
        }
    }
}

/**
 * Stub for the composite build. Dokka is only needed in the root build
 * where API docs are generated; this avoids adding a Dokka dependency
 * to the composite build's buildSrc.
 */
fun Project.configureDokkaForJvm() {
    // No-op: composite build does not generate Dokka for codegen-core
}

fun Project.configureJvmPublishing() {
    plugins.apply("maven-publish")

    val sourcesJar = tasks.register("sourcesJar", Jar::class.java) {
        archiveClassifier.set("sources")
        from(file("src/main/java"), file("src/main/kotlin"))
    }

    extensions.getByType(PublishingExtension::class.java).publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components.getByName("java"))
            artifact(sourcesJar)
            pom {
                name.set("Retrofit GraphQL")
                description.set("Retrofit GraphQL standalone JVM artifact")
                url.set("https://github.com/anitrend/retrofit-graphql")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("wax911")
                        name.set("Maxwell Mapako")
                        organizationUrl.set("https://github.com/anitrend")
                    }
                }
            }
        }
    }
}

private object StandaloneBuildCoordinates {
    private const val GROUP = "co.anitrend"

    fun group(): String = GROUP

    fun version(project: Project): String {
        val properties = Properties()
        val candidates = listOf(
            File(project.rootDir, "gradle/version.properties"),
            File(project.rootDir, "../gradle/version.properties"),
        )
        val versionFile = candidates.firstOrNull(File::exists)
            ?: error("Unable to locate gradle/version.properties from ${project.rootDir}")
        versionFile.inputStream().use(properties::load)
        return properties.getProperty("version")
            ?: error("Missing 'version' in $versionFile")
    }
}

fun Project.applyStandaloneBuildCoordinates() {
    group = StandaloneBuildCoordinates.group()
    version = StandaloneBuildCoordinates.version(this)
}
