package co.anitrend.retrofit.graphql.codegen

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists

class PublishedConsumerPluginResolutionTest {
    @Test
    fun `published plugin resolves from mavenLocal and generates sources`() {
        val pluginVersion = readStandaloneVersion()
        val projectDir = createTempDirectory("retrofit-graphql-published-consumer-")

        writeFile(
            projectDir.resolve("settings.gradle.kts"),
            """
            pluginManagement {
                plugins {
                    kotlin("jvm") version "2.4.0"
                    id("co.anitrend.retrofit.graphql.codegen") version "$pluginVersion"
                }
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                    mavenCentral()
                    google()
                }
            }

            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    mavenLocal()
                    mavenCentral()
                    google()
                }
            }

            rootProject.name = "published-consumer"
            """.trimIndent(),
        )
        writeFile(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins {
                kotlin("jvm")
                id("co.anitrend.retrofit.graphql.codegen")
            }

            retrofitGraphQL {
                packageName.set("sample.generated")
                operations.from(fileTree("src/main/graphql") {
                    include("**/*.graphql")
                })
            }
            """.trimIndent(),
        )
        writeFile(
            projectDir.resolve("src/main/graphql/GetViewer.graphql"),
            "query GetViewer { viewer { login } }",
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("generateGraphQLSources", "--stacktrace")
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSources")?.outcome)
        assertTrue(
            projectDir.resolve("build/generated/source/graphql/sample/generated/GeneratedGraphQLRegistry.kt").exists(),
        )
    }

    private fun readStandaloneVersion(): String {
        val properties = Properties()
        val versionFile = Paths.get(System.getProperty("user.dir"), "../gradle/version.properties").normalize()
        versionFile.toFile().inputStream().use(properties::load)
        return properties.getProperty("version")
    }

    private fun writeFile(
        path: Path,
        content: String,
    ) {
        path.parent?.createDirectories()
        Files.writeString(path, content)
    }
}
