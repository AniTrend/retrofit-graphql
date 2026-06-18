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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
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
            val targetMap = extension.targets.asMap

            if (targetMap.isEmpty()) {
                // No targets configured at all -- create a default "main" task
                val defaultTarget = extension.targets.maybeCreate("main")
                createTargetTask(
                    project,
                    extension,
                    "main",
                    defaultTarget,
                    "generateGraphQLSources",
                )
            } else {
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
            task.outputDir.convention(defaultOutputDir)
            task.outputDir.set(target.outputDir)

            // Generation flags: target value falls back to common value
            task.generateOperationConstants.set(
                target.generateOperationConstants.orElse(extension.common.generateOperationConstants),
            )
            task.generateDocuments.set(
                target.generateDocuments.orElse(extension.common.generateDocuments),
            )
            task.generateHashes.set(
                target.generateHashes.orElse(extension.common.generateHashes),
            )
            task.generateVariables.set(
                target.generateVariables.orElse(extension.common.generateVariables),
            )

            task.scalarMappings.set(
                project.provider { target.scalarMappings.toMap() },
            )
        }

        // Wire generated sources into the appropriate source set
        wireSourceSet(project, taskProvider)

        // Wire compile tasks to depend on generation
        project.tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }
            .configureEach { compileTask ->
                compileTask.dependsOn(taskProvider)
            }
    }

    private fun wireSourceSet(
        project: Project,
        taskProvider: TaskProvider<GenerateGraphQLSourcesTask>,
    ) {
        // Android Library
        project.plugins.withId("com.android.library") {
            val android = project.extensions.getByName("android") as LibraryExtension
            android.sourceSets.getByName("main").java.srcDir(
                taskProvider.flatMap { it.outputDir },
            )
        }

        // Android Application
        project.plugins.withId("com.android.application") {
            val android = project.extensions.getByName("android") as AppExtension
            android.sourceSets.getByName("main").java.srcDir(
                taskProvider.flatMap { it.outputDir },
            )
        }

        // Kotlin JVM
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
            sourceSets?.getByName("main")?.java?.srcDir(
                taskProvider.flatMap { it.outputDir },
            )
        }
    }
}
