/**
 * Copyright 2026 AniTrend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.anitrend.retrofit.graphql.codegen

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider

//
// Gradle plugin that registers code generation tasks for .graphql operation files.
//
// Supports multi-target DSL:
// ```kotlin
// plugins {
//     id("co.anitrend.retrofit.graphql.codegen")
// }
//
// retrofitGraphQL {
//     common {
//         generateDocuments.set(true)
//         generateOperationConstants.set(true)
//         generateHashes.set(true)
//         generateVariables.set(false)
//     }
//
//     target("anilist") {
//         packageName.set("co.anitrend.graphql.generated.anilist")
//         schema.set(file("src/main/graphql/anilist/schema.graphql"))
//         operations.from(fileTree("src/main/graphql/anilist") {
//             include("**/*.graphql")
//         })
//     }
// }
// ```
//
// Also supports legacy single-target mode (backward compatible):
// ```kotlin
// retrofitGraphQL {
//     packageName.set("co.anitrend.graphql.generated")
//     schema.set(file("src/main/graphql/schema.graphql"))
//     operations.from(fileTree("src/main/graphql") {
//         include("**/*.graphql")
//     })
// }
// ```
class RetrofitGraphQLPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                "retrofitGraphQL",
                RetrofitGraphQLExtension::class.java,
            )

        // Create common extension
        extension.common = project.objects.newInstance(CommonExtension::class.java)

        // Create targets named domain object container
        extension.targets = project.container(GraphQLTargetExtension::class.java)

        project.afterEvaluate {
            val targets = extension.targets!!
            val targetMap = targets.asMap

            if (targetMap.isEmpty()) {
                // No targets configured at all -- create a default "main" target
                // and copy legacy property values from the extension
                val defaultTarget = targets.create("main")
                copyLegacyProperties(extension, defaultTarget)
                createTargetTask(
                    project,
                    extension,
                    "main",
                    defaultTarget,
                    "generateGraphQLSources",
                )
            } else {
                // Copy legacy properties to the "main" target if present
                val mainTarget = targetMap["main"]
                if (mainTarget != null) {
                    copyLegacyProperties(extension, mainTarget)
                }
                targetMap.forEach { (name, target) ->
                    val taskName =
                        if (name == "main") {
                            "generateGraphQLSources"
                        } else {
                            "generateGraphQLSources${name.replaceFirstChar { it.uppercase() }}"
                        }
                    createTargetTask(project, extension, name, target, taskName)
                }
            }
        }
    }

    /**
     * Copies legacy-style property values from the extension to a target.
     * Only copies when the extension property has been explicitly set (not default).
     */
    private fun copyLegacyProperties(
        extension: RetrofitGraphQLExtension,
        target: GraphQLTargetExtension,
    ) {
        if (extension.packageName.isPresent) {
            target.packageName.set(extension.packageName)
        }
        if (!extension.operations.isEmpty) {
            target.operations.from(extension.operations)
        }
        if (extension.schema.isPresent) {
            target.schema.set(extension.schema)
        }
        if (extension.generateOperationConstants.isPresent) {
            target.generateOperationConstants.set(extension.generateOperationConstants)
        }
        if (extension.generateDocuments.isPresent) {
            target.generateDocuments.set(extension.generateDocuments)
        }
        if (extension.generateHashes.isPresent) {
            target.generateHashes.set(extension.generateHashes)
        }
        if (extension.generateVariables.isPresent) {
            target.generateVariables.set(extension.generateVariables)
        }
        if (extension.outputDir.isPresent) {
            target.outputDir.set(extension.outputDir)
        }
        // Copy scalar mappings
        extension.scalarMappings.toMap().forEach { (k, v) ->
            target.scalarMappings.map(k, v)
        }
    }

    private fun createTargetTask(
        project: Project,
        extension: RetrofitGraphQLExtension,
        targetName: String,
        target: GraphQLTargetExtension,
        taskName: String,
    ) {
        val taskProvider =
            project.tasks.register(
                taskName,
                GenerateGraphQLSourcesTask::class.java,
            )

        taskProvider.configure { task ->
            task.packageName.set(target.packageName)
            task.operationsDir.from(target.operations)
            task.schemaFile.set(target.schema)

            // Default output directory per target.
            // For the legacy "main" target, use the simpler path for backward compatibility.
            val defaultOutputDir =
                if (targetName == "main") {
                    project.layout.buildDirectory.dir("generated/source/graphql")
                } else {
                    project.layout.buildDirectory.dir("generated/source/graphql/$targetName")
                }
            if (target.outputDir.isPresent) {
                task.outputDir.set(target.outputDir)
            } else {
                task.outputDir.set(defaultOutputDir)
            }

            // Generation flags: target value falls back to common value
            val common = extension.common
            task.generateOperationConstants.set(
                target.generateOperationConstants.orElse(common.generateOperationConstants),
            )
            task.generateDocuments.set(
                target.generateDocuments.orElse(common.generateDocuments),
            )
            task.generateHashes.set(
                target.generateHashes.orElse(common.generateHashes),
            )
            task.generateVariables.set(
                target.generateVariables.orElse(common.generateVariables),
            )

            task.scalarMappings.set(
                project.provider { target.scalarMappings.toMap() },
            )
        }

        // Wire generated sources into the appropriate source set
        wireSourceSet(project, taskProvider)

        // Wire Kotlin compilation and symbol-processing tasks to depend on generation.
        project.tasks.configureEach { task ->
            if (shouldDependOnGraphQLGeneration(task.name)) {
                task.dependsOn(taskProvider)
            }
        }
    }

    private fun shouldDependOnGraphQLGeneration(taskName: String): Boolean {
        return isKotlinCompileTask(taskName) ||
            isKspKotlinTask(taskName) ||
            isKaptGenerateStubsKotlinTask(taskName)
    }

    private fun isKotlinCompileTask(taskName: String): Boolean {
        return taskName.startsWith("compile") && taskName.contains("Kotlin")
    }

    private fun isKspKotlinTask(taskName: String): Boolean {
        return taskName.startsWith("ksp") && taskName.contains("Kotlin")
    }

    private fun isKaptGenerateStubsKotlinTask(taskName: String): Boolean {
        return taskName.startsWith("kaptGenerateStubs") && taskName.contains("Kotlin")
    }

    private fun wireSourceSet(
        project: Project,
        taskProvider: TaskProvider<GenerateGraphQLSourcesTask>,
    ) {
        // Resolve the generated sources directory at configuration time.
        // AGP 9.x rejects Provider<Directory> in srcDir(), so resolve to a concrete File.
        val generatedDir = taskProvider.get().outputDir.get().asFile

        // Android Library
        project.plugins.withId("com.android.library") {
            val android = project.extensions.getByName("android") as LibraryExtension
            android.sourceSets.getByName("main").java.srcDir(generatedDir)
            android.sourceSets.getByName("main").kotlin.srcDir(generatedDir)
        }

        // Android Application
        project.plugins.withId("com.android.application") {
            val android = project.extensions.getByName("android") as ApplicationExtension
            android.sourceSets.getByName("main").java.srcDir(generatedDir)
            android.sourceSets.getByName("main").kotlin.srcDir(generatedDir)
        }

        // Kotlin JVM
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            wireKotlinJvmSourceSet(project, generatedDir)
            val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
            sourceSets?.getByName("main")?.java?.srcDir(generatedDir)
        }
    }

    private fun wireKotlinJvmSourceSet(
        project: Project,
        generatedDir: java.io.File,
    ) {
        val kotlinExtension =
            project.extensions.findByName("kotlin") ?: return project.logger.warn(
                "retrofit-graphql: unable to wire generated Kotlin sources because the 'kotlin' extension was not found.",
            )
        val sourceSets =
            kotlinExtension.javaClass.methods
                .firstOrNull { it.name == "getSourceSets" && it.parameterCount == 0 }
                ?.invoke(kotlinExtension)
                ?: return project.logger.warn(
                    "retrofit-graphql: unable to wire generated Kotlin sources because kotlin source sets were not accessible.",
                )
        val mainSourceSet =
            sourceSets.javaClass.methods
                .firstOrNull { it.name == "getByName" && it.parameterCount == 1 }
                ?.invoke(sourceSets, "main")
                ?: return project.logger.warn(
                    "retrofit-graphql: unable to wire generated Kotlin sources because the main Kotlin source set was not found.",
                )
        val kotlinSources =
            mainSourceSet.javaClass.methods
                .firstOrNull { it.name == "getKotlin" && it.parameterCount == 0 }
                ?.invoke(mainSourceSet)
                ?: return project.logger.warn(
                    "retrofit-graphql: unable to wire generated Kotlin sources because the Kotlin source directory API was not found.",
                )

        val srcDirMethod =
            kotlinSources.javaClass.methods
                .firstOrNull { it.name == "srcDir" && it.parameterCount == 1 }
                ?: return project.logger.warn(
                    "retrofit-graphql: unable to wire generated Kotlin sources because srcDir(any) was not available on the Kotlin source directory API.",
                )

        srcDirMethod.invoke(kotlinSources, generatedDir)
    }
}
