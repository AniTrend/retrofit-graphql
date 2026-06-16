package co.anitrend.retrofit.graphql.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.register

/**
 * Gradle plugin that registers a code generation task for .graphql operation files.
 *
 * Apply with:
 * ```kotlin
 * plugins {
 *     id("co.anitrend.retrofit.graphql.codegen")
 * }
 * ```
 */
class RetrofitGraphQLPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "retrofitGraphQL",
            RetrofitGraphQLExtension::class.java,
        )

        // Wire the output directory default to build/generated/source/graphql
        extension.outputDir.convention(
            project.layout.buildDirectory.dir("generated/source/graphql")
        )

        val generateTask = project.tasks.register<GenerateGraphQLSourcesTask>(
            "generateGraphQLSources"
        ) {
            packageName.set(extension.packageName)
            operationsDir.setFrom(extension.operations)
            outputDir.set(extension.outputDir)
            generateOperationConstants.set(extension.generateOperationConstants)
            generateDocuments.set(extension.generateDocuments)
            generateHashes.set(extension.generateHashes)
        }

        // Add generated sources to the main Kotlin source set so they are compiled
        project.afterEvaluate {
            val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
            sourceSets?.named("main") { mainSourceSet ->
                mainSourceSet.java.srcDir(extension.outputDir)
            }

            // Wire the generate task to run before Kotlin compilation
            project.tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }
                .configureEach {
                    dependsOn(generateTask)
                }
        }
    }
}
