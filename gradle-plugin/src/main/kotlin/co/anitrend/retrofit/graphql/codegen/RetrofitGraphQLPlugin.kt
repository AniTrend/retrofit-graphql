package co.anitrend.retrofit.graphql.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

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

        val generateTask = project.tasks.register(
            "generateGraphQLSources",
            GenerateGraphQLSourcesTask::class.java,
        ).get()
        generateTask.packageName.set(extension.packageName)
        generateTask.operationsDir.from(extension.operations)
        generateTask.schemaFile.set(extension.schema)
        generateTask.outputDir.set(extension.outputDir)
        generateTask.generateOperationConstants.set(extension.generateOperationConstants)
        generateTask.generateDocuments.set(extension.generateDocuments)
        generateTask.generateHashes.set(extension.generateHashes)
        generateTask.generateVariables.set(extension.generateVariables)
        generateTask.scalarMappings.set(
            project.provider { extension.scalarMappings.toMap() }
        )

        // Add generated sources to the main Kotlin source set so they are compiled
        project.afterEvaluate {
            val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
            sourceSets?.getByName("main")?.java?.srcDir(extension.outputDir)

            // Wire the generate task to run before Kotlin compilation
            project.tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }
                .configureEach { task ->
                    task.dependsOn(generateTask)
                }
        }
    }
}
