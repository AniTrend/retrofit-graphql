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

import co.anitrend.retrofit.graphql.codegen.config.SerializationBackend
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

/**
 * Determinism, caching, up-to-date, and multi-target functional tests
 * for [GenerateGraphQLSourcesTask].
 */
class GenerateGraphQLSourcesTaskFunctionalTest {

    // ------------------------------------------------------------------
    // 8.1 - Deterministic and cacheable task
    // ------------------------------------------------------------------

    @Test
    fun `clean build generates expected output`() {
        val projectDir = createProject("clean-build")

        writeSingleTargetProject(
            projectDir = projectDir,
            packageName = "sample.generated",
            schema = false,
        )
        writeGraphQLFile(projectDir, "GetViewer.graphql", "query GetViewer { viewer { login } }")

        val result = gradleRunner(projectDir, "generateGraphQLSources").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSources")?.outcome)
        assertTrue(projectDir.resolve("build/generated/source/graphql/sample/generated/GraphQLOperations.kt").exists())
        assertTrue(projectDir.resolve("build/generated/source/graphql/sample/generated/GraphQLDocuments.kt").exists())
        assertTrue(projectDir.resolve("build/generated/source/graphql/sample/generated/GraphQLHashes.kt").exists())
    }

    @Test
    fun `task is up-to-date on second run with no changes`() {
        val projectDir = createProject("up-to-date")

        writeSingleTargetProject(projectDir)
        writeGraphQLFile(projectDir, "GetViewer.graphql", "query GetViewer { viewer { login } }")

        val first = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":generateGraphQLSources")?.outcome)

        val second = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":generateGraphQLSources")?.outcome)
    }

    @Test
    fun `task re-runs when operation file changes`() {
        val projectDir = createProject("changed-op")

        writeSingleTargetProject(projectDir)
        val file = writeGraphQLFile(projectDir, "GetViewer.graphql", "query GetViewer { viewer { login } }")

        val first = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":generateGraphQLSources")?.outcome)

        // Modify the .graphql file
        Files.writeString(file, "query GetViewer { viewer { login name } }")

        val second = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, second.task(":generateGraphQLSources")?.outcome)
    }

    @Test
    fun `task re-runs when schema file changes`() {
        val projectDir = createProject("changed-schema")

        val schemaContent = """
            schema { query: Query }
            type Query { viewer: User! }
            type User { login: String! }
        """.trimIndent()
        writeSingleTargetProject(projectDir, schemaContent = schemaContent)
        writeGraphQLFile(projectDir, "GetViewer.graphql", "query GetViewer { viewer { login } }")

        val first = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":generateGraphQLSources")?.outcome)

        // Modify the schema file
        val schemaFile = projectDir.resolve("src/main/graphql/schema.graphql")
        Files.writeString(
            schemaFile,
            """
            schema { query: Query }
            type Query { viewer: User! }
            type User { login: String! name: String }
            """.trimIndent(),
        )

        val second = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, second.task(":generateGraphQLSources")?.outcome)
    }

    @Test
    fun `task re-runs when operation file is removed`() {
        val projectDir = createProject("removed-op")

        // Enable generateVariables so individual request helper files are generated
        val generatedDir = projectDir.resolve("build/generated/source/graphql/sample/generated")
        writeSingleTargetProject(
            projectDir = projectDir,
            generateVariables = true,
        )
        val fileA = writeGraphQLFile(projectDir, "GetViewer.graphql", "query GetViewer { viewer { login } }")
        writeGraphQLFile(projectDir, "SearchRepos.graphql", "query SearchRepos(\$q: String!) { search(query: \$q, type: REPOSITORY, first: 10) { edges { node { ... on Repository { name } } } } }")

        val first = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":generateGraphQLSources")?.outcome)
        assertTrue(generatedDir.resolve("GetViewer.kt").exists())
        assertTrue(generatedDir.resolve("SearchRepos.kt").exists())

        // Delete one operation file
        Files.delete(fileA)

        val second = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, second.task(":generateGraphQLSources")?.outcome)
        // The stale output for GetViewer should be gone (deleteRecursively cleans everything)
        assertFalse(generatedDir.resolve("GetViewer.kt").exists())
        // Remaining operation should still be regenerated
        assertTrue(generatedDir.resolve("SearchRepos.kt").exists())
    }

    @Test
    fun `task removes stale output when only operation file is removed`() {
        val projectDir = createProject("removed-only-op")
        val generatedDir = projectDir.resolve("build/generated/source/graphql/sample/generated")

        writeSingleTargetProject(
            projectDir = projectDir,
            generateVariables = true,
        )
        val file = writeGraphQLFile(projectDir, "GetViewer.graphql", "query GetViewer { viewer { login } }")

        val first = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":generateGraphQLSources")?.outcome)
        assertTrue(generatedDir.resolve("GetViewer.kt").exists())

        Files.delete(file)

        val second = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, second.task(":generateGraphQLSources")?.outcome)
        assertTrue(
            "Generated package directory should be removed or empty",
            !generatedDir.exists() || generatedDir.listDirectoryEntries().isEmpty(),
        )
    }

    @Test
    fun `GSON generateResponses succeeds for concrete operation and emits SerializedName DTOs`() {
        val projectDir = createProject("gson-responses-concrete")
        val generatedData = projectDir.resolve("build/generated/source/graphql/sample/generated/GetViewerData.kt")

        writeSingleTargetProject(
            projectDir = projectDir,
            schemaContent = responseSchema(),
            serializationBackend = SerializationBackend.GSON,
            generateResponses = true,
        )
        writeGraphQLFile(projectDir, "GetViewer.graphql", "query GetViewer { viewer { id login } }")

        val result = gradleRunner(projectDir, "generateGraphQLSources").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSources")?.outcome)
        assertTrue(generatedData.exists())
        val source = generatedData.readText()
        assertTrue(source.contains("import com.google.gson.annotations.SerializedName"))
        assertTrue(source.contains("@SerializedName(\"viewer\")"))
        assertTrue(source.contains("@SerializedName(\"id\")"))
        assertTrue(source.contains("@SerializedName(\"login\")"))
    }

    @Test
    fun `GSON generateResponses fails for interface operation with operation name and path`() {
        val projectDir = createProject("gson-responses-interface")

        writeSingleTargetProject(
            projectDir = projectDir,
            schemaContent = responseSchema(),
            serializationBackend = SerializationBackend.GSON,
            generateResponses = true,
        )
        writeGraphQLFile(projectDir, "GetNode.graphql", "query GetNode { node { id ... on User { login } } }")

        val result = gradleRunner(projectDir, "generateGraphQLSources").buildAndFail()

        assertTrue(result.output.contains("Operation 'GetNode'"))
        assertTrue(result.output.contains("node (Node)"))
    }

    @Test
    fun `GSON generateResponses fails for union operation with operation name and path`() {
        val projectDir = createProject("gson-responses-union")

        writeSingleTargetProject(
            projectDir = projectDir,
            schemaContent = responseSchema(),
            serializationBackend = SerializationBackend.GSON,
            generateResponses = true,
        )
        writeGraphQLFile(
            projectDir,
            "Search.graphql",
            "query Search { search { ... on User { login } ... on Repository { name } } }",
        )

        val result = gradleRunner(projectDir, "generateGraphQLSources").buildAndFail()

        assertTrue(result.output.contains("Operation 'Search'"))
        assertTrue(result.output.contains("search (SearchResult)"))
    }

    @Test
    fun `GSON generateResponses mixed target fails deterministically for abstract operation`() {
        val projectDir = createProject("gson-responses-mixed")

        writeSingleTargetProject(
            projectDir = projectDir,
            schemaContent = responseSchema(),
            serializationBackend = SerializationBackend.GSON,
            generateResponses = true,
        )
        writeGraphQLFile(projectDir, "Concrete.graphql", "query GetViewer { viewer { id login } }")
        writeGraphQLFile(projectDir, "Abstract.graphql", "query GetNode { node { id ... on User { login } } }")

        val result = gradleRunner(projectDir, "generateGraphQLSources").buildAndFail()

        assertTrue(result.output.contains("GSON response generation is not supported"))
        assertTrue(result.output.contains("Operation 'GetNode'"))
        assertTrue(result.output.contains("node (Node)"))
        assertFalse(result.output.contains("Operation 'GetViewer' has abstract type"))
    }

    @Test
    fun `GSON generateResponses failure leaves no partial generated output`() {
        val projectDir = createProject("gson-responses-no-partial")
        val generatedDir = projectDir.resolve("build/generated/source/graphql/sample/generated")

        writeSingleTargetProject(
            projectDir = projectDir,
            schemaContent = responseSchema(),
            serializationBackend = SerializationBackend.GSON,
            generateResponses = true,
        )
        writeGraphQLFile(
            projectDir,
            "Search.graphql",
            "query Search { search { ... on User { login } ... on Repository { name } } }",
        )

        val result = gradleRunner(projectDir, "generateGraphQLSources").buildAndFail()

        assertTrue(result.output.contains("Operation 'Search'"))
        assertFalse(
            "Failed validation must not leave generated package files behind",
            generatedDir.exists() && generatedDir.listDirectoryEntries().isNotEmpty(),
        )
        assertFalse(
            "Response enum file must not be written before Gson validation succeeds",
            generatedDir.resolve("Status.kt").exists(),
        )
    }

    @Test
    fun `GSON generateResponses reports nested abstract response path`() {
        val projectDir = createProject("gson-responses-nested-interface")

        writeSingleTargetProject(
            projectDir = projectDir,
            schemaContent = responseSchema(),
            serializationBackend = SerializationBackend.GSON,
            generateResponses = true,
        )
        writeGraphQLFile(
            projectDir,
            "GetViewerBestFriend.graphql",
            "query GetViewerBestFriend { viewer { id bestFriend { id ... on Repository { name } } } }",
        )

        val result = gradleRunner(projectDir, "generateGraphQLSources").buildAndFail()

        assertTrue(result.output.contains("Operation 'GetViewerBestFriend'"))
        assertTrue(result.output.contains("viewer.bestFriend (Node)"))
    }

    @Test
    fun `task re-runs when serialization backend changes`() {
        val projectDir = createProject("backend-change")

        writeSingleTargetProject(
            projectDir = projectDir,
            serializationBackend = SerializationBackend.NONE,
        )
        writeGraphQLFile(projectDir, "GetViewer.graphql", "query GetViewer { viewer { login } }")

        val first = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":generateGraphQLSources")?.outcome)

        // Change serializationBackend
        val buildFile = projectDir.resolve("build.gradle.kts")
        val updatedContent = buildFile.readText()
            .replace("SerializationBackend.NONE", "SerializationBackend.KOTLINX")
        Files.writeString(buildFile, updatedContent)

        val second = gradleRunner(projectDir, "generateGraphQLSources").build()
        assertEquals(TaskOutcome.SUCCESS, second.task(":generateGraphQLSources")?.outcome)
    }

    // ------------------------------------------------------------------
    // 8.2 - Cache restore (build cache)
    // ------------------------------------------------------------------

    @Test
    fun `task restores from build cache with FROM_CACHE outcome`() {
        val projectDir = createProject("cache-restore")

        writeSingleTargetProject(projectDir)
        writeGraphQLFile(projectDir, "GetViewer.graphql", "query GetViewer { viewer { login } }")

        // Enable build cache for the test project
        writeFile(
            projectDir.resolve("gradle.properties"),
            "org.gradle.caching=true\n",
        )

        // First build -- populates the local build cache
        val first = gradleRunner(projectDir, "generateGraphQLSources", "--build-cache").build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":generateGraphQLSources")?.outcome)

        // Clean and rebuild -- should restore from cache
        val second = gradleRunner(projectDir, "clean", "generateGraphQLSources", "--build-cache").build()
        assertEquals(TaskOutcome.FROM_CACHE, second.task(":generateGraphQLSources")?.outcome)
    }

    // ------------------------------------------------------------------
    // 8.4 - Multi-target configuration
    // ------------------------------------------------------------------

    @Test
    fun `two targets produce non-overlapping output in separate directories`() {
        val projectDir = createProject("multi-target-output")

        writeMultiTargetProject(projectDir)

        writeFile(
            projectDir.resolve("src/main/graphql/github/GetGithubViewer.graphql"),
            "query GetGithubViewer { viewer { login } }",
        )
        writeFile(
            projectDir.resolve("src/main/graphql/gitlab/GetGitlabViewer.graphql"),
            "query GetGitlabViewer { viewer { login } }",
        )

        val result = gradleRunner(projectDir, "generateGraphQLSourcesGithub", "generateGraphQLSourcesGitlab").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSourcesGithub")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSourcesGitlab")?.outcome)

        // Verify separate output directories
        val githubRegistry = projectDir.resolve("build/generated/source/graphql/github/sample/generated/github/GeneratedGraphQLRegistry.kt")
        val gitlabRegistry = projectDir.resolve("build/generated/source/graphql/gitlab/sample/generated/gitlab/GeneratedGraphQLRegistry.kt")
        assertTrue(githubRegistry.exists())
        assertTrue(gitlabRegistry.exists())

        // Verify different package content
        val githubContent = githubRegistry.readText()
        val gitlabContent = gitlabRegistry.readText()
        assertNotEquals(githubContent, gitlabContent)
    }

    @Test
    fun `common configuration is inherited by targets`() {
        val projectDir = createProject("common-config")

        writeFile(
            projectDir.resolve("settings.gradle.kts"),
            settingsFile(),
        )
        writeFile(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("co.anitrend.retrofit.graphql.codegen")
            }

            ${javaReleaseBlock()}

            retrofitGraphQL {
                common {
                    generateHashes.set(false)
                }
                target("one") {
                    packageName.set("sample.generated.one")
                    operations.from(fileTree("src/main/graphql/one") {
                        include("**/*.graphql")
                    })
                }
                target("two") {
                    packageName.set("sample.generated.two")
                    operations.from(fileTree("src/main/graphql/two") {
                        include("**/*.graphql")
                    })
                }
            }
            """.trimIndent(),
        )
        writeGraphQLSupportSources(projectDir)
        writeFile(
            projectDir.resolve("src/main/graphql/one/OpOne.graphql"),
            "query OpOne { viewer { login } }",
        )
        writeFile(
            projectDir.resolve("src/main/graphql/two/OpTwo.graphql"),
            "query OpTwo { viewer { login } }",
        )

        val result = gradleRunner(projectDir, "generateGraphQLSourcesOne", "generateGraphQLSourcesTwo").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSourcesOne")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSourcesTwo")?.outcome)

        // Both targets should NOT have generated hashes (inherited from common)
        assertFalse(
            projectDir.resolve("build/generated/source/graphql/one/sample/generated/one/GraphQLHashes.kt").exists(),
        )
        assertFalse(
            projectDir.resolve("build/generated/source/graphql/two/sample/generated/two/GraphQLHashes.kt").exists(),
        )
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun writeSingleTargetProject(
        projectDir: Path,
        packageName: String = "sample.generated",
        serializationBackend: SerializationBackend = SerializationBackend.NONE,
        schema: Boolean = false,
        schemaContent: String = "",
        generateVariables: Boolean = false,
        generateResponses: Boolean = false,
    ) {
        writeFile(projectDir.resolve("settings.gradle.kts"), settingsFile())
        val schemaBlock = if (schema || schemaContent.isNotEmpty()) {
            if (schemaContent.isNotEmpty()) {
                val schemaDir = projectDir.resolve("src/main/graphql")
                schemaDir.createDirectories()
                Files.writeString(schemaDir.resolve("schema.graphql"), schemaContent)
            }
            """
            schema.set(file("src/main/graphql/schema.graphql"))
            generateVariables.set(true)
            """.trimIndent()
        } else {
            ""
        }

        val extraSettings = listOfNotNull(
            "generateVariables.set(true)".takeIf { generateVariables },
            "generateResponses.set(true)".takeIf { generateResponses },
        ).joinToString("\n")

        writeFile(
            projectDir.resolve("build.gradle.kts"),
            """
            import co.anitrend.retrofit.graphql.codegen.config.SerializationBackend

            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("co.anitrend.retrofit.graphql.codegen")
            }

            ${javaReleaseBlock()}

            retrofitGraphQL {
                packageName.set("$packageName")
                operations.from(fileTree("src/main/graphql") {
                    include("**/*.graphql")
                })
                $schemaBlock
                $extraSettings
                serializationBackend.set(SerializationBackend.$serializationBackend)
            }
            """.trimIndent(),
        )
        writeGraphQLSupportSources(projectDir)
    }

    private fun writeMultiTargetProject(projectDir: Path) {
        writeFile(projectDir.resolve("settings.gradle.kts"), settingsFile())
        writeGraphQLSupportSources(projectDir)
        writeFile(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("co.anitrend.retrofit.graphql.codegen")
            }

            ${javaReleaseBlock()}

            retrofitGraphQL {
                target("github") {
                    packageName.set("sample.generated.github")
                    operations.from(fileTree("src/main/graphql/github") {
                        include("**/*.graphql")
                    })
                }

                target("gitlab") {
                    packageName.set("sample.generated.gitlab")
                    operations.from(fileTree("src/main/graphql/gitlab") {
                        include("**/*.graphql")
                    })
                }
            }
            """.trimIndent(),
        )
    }

    private fun writeGraphQLFile(
        projectDir: Path,
        fileName: String,
        content: String,
    ): Path {
        val file = projectDir.resolve("src/main/graphql/$fileName")
        file.parent.createDirectories()
        Files.writeString(file, content)
        return file
    }

    private fun gradleRunner(
        projectDir: Path,
        vararg arguments: String,
    ): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(*arguments, "--stacktrace")
            .withPluginClasspath()

    private fun createProject(name: String): Path =
        createTempDirectory("retrofit-graphql-$name-")

    private fun responseSchema(): String =
        """
        schema { query: Query }
        type Query {
            viewer: User!
            node: Node!
            search: SearchResult!
        }
        interface Node { id: ID! }
        type User implements Node {
            id: ID!
            login: String!
            status: Status!
            bestFriend: Node!
        }
        type Repository implements Node {
            id: ID!
            name: String!
        }
        union SearchResult = User | Repository
        enum Status { ACTIVE INACTIVE }
        """.trimIndent()

    private fun writeGraphQLSupportSources(projectDir: Path) {
        writeFile(
            projectDir.resolve("src/main/kotlin/co/anitrend/retrofit/graphql/model/GraphQLDocumentRegistry.kt"),
            """
            package co.anitrend.retrofit.graphql.model

            interface GraphQLDocumentRegistry {
                fun document(operationName: String): String?
                fun hash(operationName: String): String?
            }
            """.trimIndent(),
        )
    }

    private fun writeFile(
        path: Path,
        content: String,
    ) {
        path.parent?.createDirectories()
        Files.writeString(path, content)
    }

    private fun settingsFile(): String =
        """
        pluginManagement {
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
            }
            repositories {
                gradlePluginPortal()
                mavenCentral()
            }
        }

        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                mavenCentral()
            }
        }

        rootProject.name = "functional-test"
        """.trimIndent()

    private fun javaReleaseBlock(): String =
        """
        // Keep Java at 24 because host JDK 26 makes Kotlin fall back to JVM 24 in fixtures.
        tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
            options.release.set(24)
        }
        """.trimIndent()

    private companion object {
        private const val KOTLIN_VERSION = "2.4.0"
    }
}
