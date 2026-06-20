package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.baseAppExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isFacadeModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isLibraryModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libraryExtension
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.props
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.publishingExtension
import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.ApplicationDefaultConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.invoke
import java.util.*

private fun Properties.applyToBuildConfigForBuild(project: Project, buildType: ApplicationBuildType) {
    forEach { propEntry ->
        val key = propEntry.key as String
        val value = propEntry.value as String
        project.logger.lifecycle("Adding build config field property -> key: $key value: $value")
        buildType.buildConfigField("String", key, value)
    }
}

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

private fun Project.configurePublishing() {
    // Tell AGP to produce a component-backed release variant.
    // The component is created during task graph resolution, *after* project
    // configuration, so the publication itself must be deferred via afterEvaluate.
    libraryExtension().run {
        publishing {
            singleVariant("release") {
                withSourcesJar()
            }
        }
    }

    // The "release" software component is the source of truth for the Maven
    // publication, which lets Gradle emit correct variant-aware POM/module metadata.
    afterEvaluate {
        publishingExtension().publications {
            create("maven", MavenPublication::class.java) {
                groupId = "co.anitrend"
                // Keep backward-compatible artifactId for the deprecated :library facade
                artifactId = if (isFacadeModule()) "retrofit-graphql" else project.name
                version = props[PropertyTypes.NAME]
                from(components.getByName("release"))

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
}

private fun Project.configureDokka() {
    // V2 Dokka configuration using the project-level dokka extension.
    // sourceLink, perPackageOption, etc. from V1 are not available in V2's
    // compatibility source set API; those need follow-up migration to V2 DSL.
    val dokkaExt = extensions.getByName("dokka") as org.jetbrains.dokka.gradle.DokkaExtension
    dokkaExt.apply {
        moduleName.set(this@configureDokka.name)

        dokkaSourceSets.configureEach {
            skipDeprecated.set(false)
            reportUndocumented.set(true)
            skipEmptyPackages.set(true)
            jdkVersion.set(21)
            sourceRoots.from(file("src"))
        }
    }
}

internal fun Project.configureOptions() {
    logger.lifecycle("Applying extension options for ${project.path}")
    if (isLibraryModule()) {
        logger.lifecycle("Applying additional tasks options for dokka and javadoc on ${project.path}")

        configureDokka()
        configurePublishing()
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
