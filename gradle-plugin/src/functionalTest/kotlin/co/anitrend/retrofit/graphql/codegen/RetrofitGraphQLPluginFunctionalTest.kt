package co.anitrend.retrofit.graphql.codegen

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists

class RetrofitGraphQLPluginFunctionalTest {
    @Test
    fun `compileKotlin sees generated sources in single target projects`() {
        val projectDir = createProject("compile-only")

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

            retrofitGraphQL {
                packageName.set("sample.generated")
                operations.from(fileTree("src/main/graphql") {
                    include("**/*.graphql")
                })
            }
            """.trimIndent(),
        )

        writeGraphQLSupportSources(projectDir)
        writeFile(
            projectDir.resolve("src/main/graphql/GetViewer.graphql"),
            "query GetViewer { viewer { login } }",
        )
        writeFile(
            projectDir.resolve("src/main/kotlin/sample/UseGeneratedRegistry.kt"),
            """
            package sample

            internal class UseGeneratedRegistry {
                val operationName: String = sample.generated.GraphQLOperations.Query.GetViewer
                val document: String? = sample.generated.GeneratedGraphQLRegistry.document(operationName)
            }
            """.trimIndent(),
        )

        val result = gradleRunner(projectDir, "compileKotlin").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
        assertTrue(
            projectDir.resolve("build/generated/source/graphql/sample/generated/GeneratedGraphQLRegistry.kt").exists(),
        )
    }

    @Test
    fun `kspKotlin sees generated sources before symbol processing`() {
        val projectDir = createProject("ksp")

        writeFile(
            projectDir.resolve("settings.gradle.kts"),
            settingsFile(
                extraPluginManagement =
                    "id(\"com.google.devtools.ksp\") version \"$KSP_VERSION\"",
                includes = "include(\":app\", \":processor\")",
            ),
        )
        writeFile(projectDir.resolve("build.gradle.kts"), "")

        writeFile(
            projectDir.resolve("app/build.gradle.kts"),
            """
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("com.google.devtools.ksp")
                id("co.anitrend.retrofit.graphql.codegen")
            }

            dependencies {
                ksp(project(":processor"))
            }

            retrofitGraphQL {
                packageName.set("sample.generated")
                operations.from(fileTree("src/main/graphql") {
                    include("**/*.graphql")
                })
            }
            """.trimIndent(),
        )
        writeGraphQLSupportSources(projectDir.resolve("app"))
        writeFile(
            projectDir.resolve("app/src/main/graphql/GetViewer.graphql"),
            "query GetViewer { viewer { login } }",
        )
        writeFile(
            projectDir.resolve("app/src/main/kotlin/sample/App.kt"),
            "package sample\n\ninternal class App",
        )

        writeFile(
            projectDir.resolve("processor/build.gradle.kts"),
            """
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
            }

            dependencies {
                implementation("com.google.devtools.ksp:symbol-processing-api:$KSP_VERSION")
            }
            """.trimIndent(),
        )
        writeFile(
            projectDir.resolve("processor/src/main/kotlin/sample/processor/GeneratedRegistrySymbolProcessorProvider.kt"),
            """
            package sample.processor

            import com.google.devtools.ksp.processing.KSPLogger
            import com.google.devtools.ksp.processing.Resolver
            import com.google.devtools.ksp.processing.SymbolProcessor
            import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
            import com.google.devtools.ksp.processing.SymbolProcessorProvider

            class GeneratedRegistrySymbolProcessorProvider : SymbolProcessorProvider {
                override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
                    return GeneratedRegistrySymbolProcessor(environment.logger)
                }
            }

            private class GeneratedRegistrySymbolProcessor(
                private val logger: KSPLogger,
            ) : SymbolProcessor {
                override fun process(resolver: Resolver): List<com.google.devtools.ksp.symbol.KSAnnotated> {
                    val generatedRegistry = resolver.getClassDeclarationByName(
                        resolver.getKSNameFromString("sample.generated.GeneratedGraphQLRegistry"),
                    )
                    if (generatedRegistry == null) {
                        logger.error("GeneratedGraphQLRegistry was not available to KSP")
                    }
                    return emptyList()
                }
            }
            """.trimIndent(),
        )
        writeFile(
            projectDir.resolve("processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider"),
            "sample.processor.GeneratedRegistrySymbolProcessorProvider",
        )

        val result = gradleRunner(projectDir, ":app:kspKotlin").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:generateGraphQLSources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":app:kspKotlin")?.outcome)
    }

    @Test
    fun `kaptGenerateStubsKotlin sees generated sources before stub generation`() {
        val projectDir = createProject("kapt")

        writeFile(
            projectDir.resolve("settings.gradle.kts"),
            settingsFile(includes = "include(\":app\", \":processor\")"),
        )
        writeFile(projectDir.resolve("build.gradle.kts"), "")

        writeFile(
            projectDir.resolve("app/build.gradle.kts"),
            """
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                kotlin("kapt") version "$KOTLIN_VERSION"
                id("co.anitrend.retrofit.graphql.codegen")
            }

            dependencies {
                compileOnly(project(":processor"))
                kapt(project(":processor"))
            }

            retrofitGraphQL {
                packageName.set("sample.generated")
                operations.from(fileTree("src/main/graphql") {
                    include("**/*.graphql")
                })
            }
            """.trimIndent(),
        )
        writeGraphQLSupportSources(projectDir.resolve("app"))
        writeFile(
            projectDir.resolve("app/src/main/graphql/GetViewer.graphql"),
            "query GetViewer { viewer { login } }",
        )
        writeFile(
            projectDir.resolve("app/src/main/kotlin/sample/UseGeneratedRegistry.kt"),
            """
            package sample

            import sample.processor.Trigger

            @Trigger
            internal class UseGeneratedRegistry {
                val document: String? = sample.generated.GeneratedGraphQLRegistry.document(
                    sample.generated.GraphQLOperations.Query.GetViewer,
                )
            }
            """.trimIndent(),
        )

        writeFile(
            projectDir.resolve("processor/build.gradle.kts"),
            """
            plugins {
                `java-library`
            }

            """.trimIndent(),
        )
        writeFile(
            projectDir.resolve("processor/src/main/java/sample/processor/Trigger.java"),
            """
            package sample.processor;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.SOURCE)
            public @interface Trigger {
            }
            """.trimIndent(),
        )
        writeFile(
            projectDir.resolve("processor/src/main/java/sample/processor/NoOpProcessor.java"),
            """
            package sample.processor;

            import java.util.Set;
            import javax.annotation.processing.AbstractProcessor;
            import javax.annotation.processing.RoundEnvironment;
            import javax.lang.model.SourceVersion;
            import javax.lang.model.element.TypeElement;

            public class NoOpProcessor extends AbstractProcessor {
                @Override
                public Set<String> getSupportedAnnotationTypes() {
                    return Set.of(Trigger.class.getCanonicalName());
                }

                @Override
                public SourceVersion getSupportedSourceVersion() {
                    return SourceVersion.latestSupported();
                }

                @Override
                public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                    return false;
                }
            }
            """.trimIndent(),
        )
        writeFile(
            projectDir.resolve("processor/src/main/resources/META-INF/services/javax.annotation.processing.Processor"),
            "sample.processor.NoOpProcessor",
        )

        val result = gradleRunner(projectDir, ":app:kaptGenerateStubsKotlin").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:generateGraphQLSources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":app:kaptGenerateStubsKotlin")?.outcome)
    }

    @Test
    fun `multiple targets wire all generated source directories into compilation`() {
        val projectDir = createProject("multi-target")

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

        writeGraphQLSupportSources(projectDir)
        writeFile(
            projectDir.resolve("src/main/graphql/github/GetGithubViewer.graphql"),
            "query GetGithubViewer { viewer { login } }",
        )
        writeFile(
            projectDir.resolve("src/main/graphql/gitlab/GetGitlabViewer.graphql"),
            "query GetGitlabViewer { viewer { login } }",
        )
        writeFile(
            projectDir.resolve("src/main/kotlin/sample/UseGeneratedTargets.kt"),
            """
            package sample

            internal class UseGeneratedTargets {
                val githubDocument = sample.generated.github.GeneratedGraphQLRegistry.document(
                    sample.generated.github.GraphQLOperations.Query.GetGithubViewer,
                )
                val gitlabDocument = sample.generated.gitlab.GeneratedGraphQLRegistry.document(
                    sample.generated.gitlab.GraphQLOperations.Query.GetGitlabViewer,
                )
            }
            """.trimIndent(),
        )

        val result = gradleRunner(projectDir, "compileKotlin").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSourcesGithub")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSourcesGitlab")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
        assertTrue(
            projectDir.resolve("build/generated/source/graphql/github/sample/generated/github/GeneratedGraphQLRegistry.kt").exists(),
        )
        assertTrue(
            projectDir.resolve("build/generated/source/graphql/gitlab/sample/generated/gitlab/GeneratedGraphQLRegistry.kt").exists(),
        )
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

    private fun settingsFile(
        extraPluginManagement: String = "",
        includes: String = "",
    ): String =
        """
        pluginManagement {
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                $extraPluginManagement
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
        $includes
        """.trimIndent()

    private companion object {
        private const val KOTLIN_VERSION = "2.4.0"
        private const val KSP_VERSION = "2.3.9"
    }
}
