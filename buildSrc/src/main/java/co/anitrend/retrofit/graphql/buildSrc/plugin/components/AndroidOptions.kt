package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.baseAppExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.baseExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isLibraryModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.props
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.publishingExtension
import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.ApplicationDefaultConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL
import java.util.*

@Suppress("UnstableApiUsage")
private fun Properties.applyToBuildConfigForBuild(project: Project, buildType: ApplicationBuildType) {
    forEach { propEntry ->
        val key = propEntry.key as String
        val value = propEntry.value as String
        project.logger.lifecycle("Adding build config field property -> key: $key value: $value")
        buildType.buildConfigField("String", key, value)
    }
}

@Suppress("UnstableApiUsage")
private fun NamedDomainObjectContainer<ApplicationBuildType>.applyConfiguration(project: Project) {
    asMap.forEach { buildTypeEntry ->
        project.logger.lifecycle("Configuring build type -> ${buildTypeEntry.key}")
        val buildType = buildTypeEntry.value

        val secretsFile = project.file(".config/secrets.properties")
        if (secretsFile.exists())
            secretsFile.inputStream().use { fis ->
                Properties().run {
                    load(fis); applyToBuildConfigForBuild(project, buildType)
                }
            }

        val configurationFile = project.file(".config/configuration.properties")
        if (configurationFile.exists())
            configurationFile.inputStream().use { fis ->
                Properties().run {
                    load(fis); applyToBuildConfigForBuild(project, buildType)
                }
            }
    }
}

@Suppress("UnstableApiUsage")
private fun ApplicationDefaultConfig.applyRoomCompilerOptions(project: Project) {
    project.logger.lifecycle("Adding java compiler options for room on module-> ${project.path}")
    javaCompileOptions {
        annotationProcessorOptions {
            arguments(
                mapOf(
                    "room.schemaLocation" to "${project.projectDir}/schemas",
                    "room.expandingProjections" to "true",
                    "room.incremental" to "true"
                )
            )
        }
    }
}

private fun Project.createMavenPublicationUsing(sourcesJar: Jar) {
    logger.lifecycle("Applying publication configuration on ${project.path}")
    publishingExtension().publications {
        val component = components.findByName("android")

        logger.lifecycle("Configuring maven publication options for ${project.path}:maven with component-> ${component?.name}")
        create("maven", MavenPublication::class.java) {
            groupId = "co.anitrend"
            artifactId = "retrofit-graphql"
            version = props[PropertyTypes.NAME]

            artifact(sourcesJar)
            artifact("${project.layout.buildDirectory.get()}/outputs/aar/${project.name}-release.aar")
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
                        organizationUrl.set("https://github.com/anitrend")
                    }
                }
            }
        }
    }
}

private fun Project.createDokkaTaskProvider() = tasks.named<DokkaTask>("dokkaHtml") {
    outputDirectory.set(buildDir.resolve("docs/dokka"))

    // Set module name displayed in the final output
    moduleName.set(this@createDokkaTaskProvider.name)

    // Use default or set to custom path to cache directory
    // to enable package-list caching
    // When this is set to default, caches are stored in $USER_HOME/.cache/dokka
    //cacheRoot.set(file("default"))

    dokkaSourceSets {
        configureEach { // Or source set name, for single-platform the default source sets are `main` and `test`
            // Used when configuring source sets manually for declaring which source sets this one depends on
            //dependsOn(dependenciesOfProject().map(Modules.Module::path))

            // Used to remove a source set from documentation, test source sets are suppressed by default
            suppress.set(false)

            // Used to prevent resolving package-lists online. When this option is set to true, only local files are resolved
            offlineMode.set(false) // this is failing in the ci env

            // Use to include or exclude non public members
            includeNonPublic.set(false)

            // Do not output deprecated members. Applies globally, can be overridden by packageOptions
            skipDeprecated.set(false)

            // Emit warnings about not documented members. Applies globally, also can be overridden by packageOptions
            reportUndocumented.set(true)

            // Do not create index pages for empty packages
            skipEmptyPackages.set(true)

            // This name will be shown in the final output
            //displayName.set("JVM")

            // Platform used for code analysis. See the "Platforms" section of this readme
            platform.set(org.jetbrains.dokka.Platform.jvm)

            // Property used for manual addition of files to the classpath
            // This property does not override the classpath collected automatically but appends to it
            // classpath.from(file("libs/dependency.jar"))

            // List of files with module and package documentation
            // https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation
            //includes.from("packages.md", "extra.md")

            // List of files or directories containing sample code (referenced with @sample tags)
            //samples.from("samples/basic.kt", "samples/advanced.kt")

            // By default, sourceRoots are taken from Kotlin Plugin and kotlinTasks, following roots will be appended to them
            // Repeat for multiple sourceRoots
            sourceRoot(file("src"))

            // Used for linking to JDK documentation
            jdkVersion.set(17)

            // Disable linking to online kotlin-stdlib documentation
            noStdlibLink.set(false)

            // Disable linking to online JDK documentation
            noJdkLink.set(false)

            // Disable linking to online Android documentation (only applicable for Android projects)
            noAndroidSdkLink.set(false)

            // Allows linking to documentation of the project"s dependencies (generated with Javadoc or Dokka)
            // Repeat for multiple links
            externalDocumentationLink {
                // Root URL of the generated documentation to link with. The trailing slash is required!
                url.set(URL("https://developer.android.com/reference/kotlin/"))

                // If package-list file is located in non-standard location
                packageListUrl.set(URL("https://developer.android.com/reference/androidx/package-list"))
            }

            // Allows to customize documentation generation options on a per-package basis
            // Repeat for multiple packageOptions
            // If multiple packages match the same matchingRegex, the longuest matchingRegex will be used
            perPackageOption {
                matchingRegex.set("kotlin($|\\.).*") // will match kotlin and all sub-packages of it
                // All options are optional, default values are below:
                skipDeprecated.set(false)
                reportUndocumented.set(true) // Emit warnings about not documented members
                includeNonPublic.set(false)
            }
            // Suppress a package
            perPackageOption {
                matchingRegex.set(".*\\.internal.*") // will match all .internal packages and sub-packages
                suppress.set(true)
            }
        }
    }
}

internal fun Project.configureOptions() {
    logger.lifecycle("Applying extension options for ${project.path}")
    if (isLibraryModule()) {
        val baseExt = baseExtension()

        logger.lifecycle("Applying additional tasks options for dokka and javadoc on ${project.path}")

        createDokkaTaskProvider()

        val sourcesJar by tasks.register("sourcesJar", Jar::class.java) {
            archiveClassifier.set("sources")
            from(baseExt.sourceSets["main"].java.srcDirs)
        }

        val classesJar by tasks.register("classesJar", Jar::class.java) {
            from("${project.layout.buildDirectory.get()}/intermediates/classes/release")
        }

        artifacts {
            add("archives", classesJar)
            add("archives", sourcesJar)
        }

        createMavenPublicationUsing(sourcesJar)
    }
    else
        baseAppExtension().run {
            defaultConfig {
                applyRoomCompilerOptions(this@configureOptions)
            }
            buildTypes {
                applyConfiguration(this@configureOptions)
            }
        }
}