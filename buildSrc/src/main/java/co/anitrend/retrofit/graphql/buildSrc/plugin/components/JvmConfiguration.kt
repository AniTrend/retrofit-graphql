package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libs
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.publishingExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.dokka.gradle.DokkaExtension

fun Project.configureSpotlessForJvm() {
    plugins.apply("com.diffplug.spotless")
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude(
                "${layout.buildDirectory.get()}/**/*.kt",
                "**/test/**/*.kt",
                "bin/**/*.kt"
            )
            ktlint(libs.pintrest.ktlint.get().version)
                .editorConfigOverride(mapOf("ktlint_standard_filename" to "disabled"))
            licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
        }
    }
}

fun Project.configureDokkaForJvm() {
    plugins.apply("org.jetbrains.dokka")
    extensions.configure<DokkaExtension> {
        moduleName.set("retrofit-graphql-${this@configureDokkaForJvm.name}")
        dokkaSourceSets.configureEach {
            skipDeprecated.set(false)
            reportUndocumented.set(true)
            skipEmptyPackages.set(true)
            jdkVersion.set(21)
            sourceRoots.from(file("src"))
        }
    }
}

fun Project.configureJvmPublishing() {
    logger.lifecycle("Applying publication configuration on ${project.path}")

    plugins.apply("maven-publish")

    val version = PropertiesReader(this)[PropertyTypes.NAME]

    val sourcesJar = tasks.register("sourcesJar", Jar::class.java) {
        archiveClassifier.set("sources")
        from(file("src/main/java"), file("src/main/kotlin"))
    }

    logger.lifecycle("Configuring maven publication options for ${project.path}:maven with component-> java")

    publishingExtension().publications {
        create("maven", MavenPublication::class.java) {
            setGroupId("co.anitrend")
            setArtifactId(project.name)
            setVersion(version)
            from(components.getByName("java"))
            artifact(sourcesJar)
            pom {
                name.set("Retrofit GraphQL")
                description.set("This is a retrofit converter which uses annotations to inject .graphql query or mutation files into a request body along with any GraphQL variables.")
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
