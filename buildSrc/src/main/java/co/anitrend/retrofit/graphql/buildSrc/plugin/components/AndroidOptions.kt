package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.jetbrains.dokka.gradle.DokkaTask
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.baseExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.androidExtensionsExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.publishingExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libraryExtension
import co.anitrend.retrofit.graphql.buildSrc.common.Versions
import co.anitrend.retrofit.graphql.buildSrc.common.isLibraryModule
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.getValue
import java.io.File
import java.net.URL

private fun Project.configureMavenPublish(javadocJar: Jar, sourcesJar: Jar) {
    println("Applying publication configuration on ${project.path}")
    publishingExtension().publications {
        val component = components.findByName("android")

        println("Configuring maven publication options for ${project.path}:maven with component-> ${component?.name}")
        create("maven", MavenPublication::class.java) {
            groupId = "co.anitrend"
            artifactId = "retrofit-graphql"
            version = Versions.versionName

            artifact(javadocJar)
            artifact(sourcesJar)
            artifact("${project.buildDir}/outputs/aar/${project.name}-release.aar")
            from(component)

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
                        email.set("mxt.developer@gmail.com")
                        organizationUrl.set("https://github.com/anitrend")
                    }
                }
            }
        }
    }
}

private fun Project.configureDokka() {
    val baseExt = baseExtension()
    val mainSourceSet = baseExt.sourceSets["main"].java.srcDirs

    println("Applying additional tasks options for dokka and javadoc on ${project.path}")

    val dokka = tasks.withType(DokkaTask::class.java) {
        outputFormat = "html"
        outputDirectory = "$buildDir/docs/javadoc"

        configuration {
            moduleName = project.name
            reportUndocumented = true
            platform = "JVM"
            jdkVersion = 8

            perPackageOption {
                prefix = "kotlin"
                skipDeprecated = false
                reportUndocumented = true
                includeNonPublic = false
            }

            sourceLink {
                path = "src/main/kotlin"
                url =
                    "https://github.com/anitrend/retrofit-graphql/tree/develop/${project.name}/src/main/kotlin"
                lineSuffix = "#L"
            }

            externalDocumentationLink {
                url = URL("https://developer.android.com/reference/kotlin/")
                packageListUrl =
                    URL("https://developer.android.com/reference/androidx/package-list")
            }
        }
    }

    val dokkaJar by tasks.register("dokkaJar", Jar::class.java) {
        archiveClassifier.set("javadoc")
        from(dokka)
    }

    val sourcesJar by tasks.register("sourcesJar", Jar::class.java) {
        archiveClassifier.set("sources")
        from(mainSourceSet)
    }

    val classesJar by tasks.register("classesJar", Jar::class.java) {
        from("${project.buildDir}/intermediates/classes/release")
    }

    val javadoc = tasks.create("javadoc", Javadoc::class.java) {
        classpath += project.files(baseExt.bootClasspath.joinToString(File.pathSeparator))
        libraryExtension().libraryVariants.forEach { variant ->
            if (variant.name == "release") {
                classpath += variant.javaCompileProvider.get().classpath
            }
        }
        exclude("**/R.html", "**/R.*.html", "**/index.html")
    }

    val javadocJar = tasks.create("javadocJar", Jar::class.java) {
        dependsOn(javadoc, dokka)
        archiveClassifier.set("javadoc")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        includeEmptyDirs = false
        from(javadoc.destinationDir, dokka.first().outputDirectory)
    }

    artifacts {
        add("archives", dokkaJar)
        add("archives", classesJar)
        add("archives", sourcesJar)
    }

    configureMavenPublish(javadocJar, sourcesJar)
}

@Suppress("UnstableApiUsage")
internal fun Project.configureOptions() {
    if (isLibraryModule())
        configureDokka()
    else {
        println("Enabling experimental extension options for feature module -> ${project.path}")
        androidExtensionsExtension().isExperimental = true
    }
}