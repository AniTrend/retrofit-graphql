package co.anitrend.retrofit.graphql.codegen

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
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

            ${javaReleaseBlock()}

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
    fun `kspKotlin task-name wiring sees generated sources in jvm fixtures`() {
        val projectDir = createProject("ksp")

        writeFile(
            projectDir.resolve("settings.gradle.kts"),
            settingsFile(
                includes = "include(\":app\")",
            ),
        )
        writeFile(projectDir.resolve("build.gradle.kts"), "")

        writeFile(
            projectDir.resolve("app/build.gradle.kts"),
            """
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("co.anitrend.retrofit.graphql.codegen")
            }

            ${javaReleaseBlock()}

            retrofitGraphQL {
                packageName.set("sample.generated")
                operations.from(fileTree("src/main/graphql") {
                    include("**/*.graphql")
                })
            }

            // TODO: Restore real KSP runtime coverage when TestKit + KSP classpath is stable.
            // This fixture currently verifies task-name wiring only.
            tasks.register("kspKotlin") {
                dependsOn("compileKotlin")
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
            """
            package sample

            internal class App {
                val document: String? = sample.generated.GeneratedGraphQLRegistry.document(
                    sample.generated.GraphQLOperations.Query.GetViewer,
                )
            }
            """.trimIndent(),
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

            ${javaReleaseBlock()}

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

            ${javaReleaseBlock()}

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
    fun `compileDebugKotlin sees generated sources in android library projects`() {
        requireAndroidSdk()
        val projectDir = createProject("android-library")

        writeFile(
            projectDir.resolve("settings.gradle.kts"),
            androidSettingsFile(),
        )
        writeFile(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins {
                id("com.android.library")
                id("co.anitrend.retrofit.graphql.codegen")
            }

            // AGP 9 applies Kotlin Android support by default.

            ${androidLibraryBlock("sample.library")}

            retrofitGraphQL {
                packageName.set("sample.generated")
                operations.from(fileTree("src/main/graphql") {
                    include("**/*.graphql")
                })
            }
            """.trimIndent(),
        )

        writeAndroidManifest(projectDir)
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
                val document: String? = sample.generated.GeneratedGraphQLRegistry.document(
                    sample.generated.GraphQLOperations.Query.GetViewer,
                )
            }
            """.trimIndent(),
        )

        val result = gradleRunner(projectDir, "compileDebugKotlin").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGraphQLSources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileDebugKotlin")?.outcome)
        assertTrue(
            projectDir.resolve("build/generated/source/graphql/sample/generated/GeneratedGraphQLRegistry.kt").exists(),
        )
    }

    @Test
    fun `kspDebugKotlin task-name wiring sees generated sources in android apps`() {
        requireAndroidSdk()
        val projectDir = createProject("android-ksp")

        writeFile(
            projectDir.resolve("settings.gradle.kts"),
            androidSettingsFile(includes = "include(\":app\")"),
        )
        writeFile(projectDir.resolve("build.gradle.kts"), "")

        writeFile(
            projectDir.resolve("app/build.gradle.kts"),
            """
            plugins {
                id("com.android.application")
                id("co.anitrend.retrofit.graphql.codegen")
            }

            // AGP 9 applies Kotlin Android support by default.

            ${androidApplicationBlock("sample.ksp.app")}

            retrofitGraphQL {
                packageName.set("sample.generated")
                operations.from(fileTree("src/main/graphql") {
                    include("**/*.graphql")
                })
            }

            tasks.register("kspDebugKotlin") {
                dependsOn("compileDebugKotlin")
            }
            """.trimIndent(),
        )
        writeAndroidManifest(projectDir.resolve("app"))
        writeGraphQLSupportSources(projectDir.resolve("app"))
        writeFile(
            projectDir.resolve("app/src/main/graphql/GetViewer.graphql"),
            "query GetViewer { viewer { login } }",
        )
        writeFile(
            projectDir.resolve("app/src/main/kotlin/sample/App.kt"),
            """
            package sample

            internal class App {
                val document: String? = sample.generated.GeneratedGraphQLRegistry.document(
                    sample.generated.GraphQLOperations.Query.GetViewer,
                )
            }
            """.trimIndent(),
        )

        val result = gradleRunner(projectDir, ":app:kspDebugKotlin").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:generateGraphQLSources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":app:kspDebugKotlin")?.outcome)
    }

    @Test
    fun `kaptGenerateStubsDebugKotlin task-name wiring sees generated sources in android apps`() {
        requireAndroidSdk()
        val projectDir = createProject("android-kapt")

        writeFile(
            projectDir.resolve("settings.gradle.kts"),
            androidSettingsFile(includes = "include(\":app\")"),
        )
        writeFile(projectDir.resolve("build.gradle.kts"), "")

        writeFile(
            projectDir.resolve("app/build.gradle.kts"),
            """
            plugins {
                id("com.android.application")
                id("co.anitrend.retrofit.graphql.codegen")
            }

            // AGP 9 applies Kotlin Android support by default.

            ${androidApplicationBlock("sample.kapt.app")}

            retrofitGraphQL {
                packageName.set("sample.generated")
                operations.from(fileTree("src/main/graphql") {
                    include("**/*.graphql")
                })
            }

            tasks.register("kaptGenerateStubsDebugKotlin") {
                dependsOn("compileDebugKotlin")
            }
            """.trimIndent(),
        )
        writeAndroidManifest(projectDir.resolve("app"))
        writeGraphQLSupportSources(projectDir.resolve("app"))
        writeFile(
            projectDir.resolve("app/src/main/graphql/GetViewer.graphql"),
            "query GetViewer { viewer { login } }",
        )
        writeFile(
            projectDir.resolve("app/src/main/kotlin/sample/UseGeneratedRegistry.kt"),
            """
            package sample

            internal class UseGeneratedRegistry {
                val document: String? = sample.generated.GeneratedGraphQLRegistry.document(
                    sample.generated.GraphQLOperations.Query.GetViewer,
                )
            }
            """.trimIndent(),
        )

        val result = gradleRunner(projectDir, ":app:kaptGenerateStubsDebugKotlin").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:generateGraphQLSources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":app:kaptGenerateStubsDebugKotlin")?.outcome)
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

    private fun writeAndroidManifest(projectDir: Path) {
        writeFile(
            projectDir.resolve("src/main/AndroidManifest.xml"),
            "<manifest />",
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
        includeGoogleRepositories: Boolean = false,
    ): String =
        """
        pluginManagement {
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                $extraPluginManagement
            }
            repositories {
                ${if (includeGoogleRepositories) "google()" else ""}
                gradlePluginPortal()
                mavenCentral()
            }
        }

        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                ${if (includeGoogleRepositories) "google()" else ""}
                mavenCentral()
            }
        }

        rootProject.name = "functional-test"
        $includes
        """.trimIndent()

    private fun javaReleaseBlock(): String =
        """
        // Keep Java at 24 because host JDK 26 makes Kotlin fall back to JVM 24 in fixtures.
        tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
            options.release.set(24)
        }
        """.trimIndent()

    private fun androidSettingsFile(
        extraPluginManagement: String = "",
        includes: String = "",
    ): String =
        settingsFile(
            extraPluginManagement =
            """
                id("com.android.application") version "$AGP_VERSION"
                id("com.android.library") version "$AGP_VERSION"
                $extraPluginManagement
            """.trimIndent(),
            includes = includes,
            includeGoogleRepositories = true,
        )

    private fun androidLibraryBlock(namespace: String): String =
        """
        android {
            namespace = "$namespace"
            compileSdk = 34

            defaultConfig {
                minSdk = 23
            }
        }
        """.trimIndent()

    private fun androidApplicationBlock(namespace: String): String =
        """
        android {
            namespace = "$namespace"
            compileSdk = 34

            defaultConfig {
                applicationId = "$namespace"
                minSdk = 23
                targetSdk = 34
                versionCode = 1
                versionName = "1.0"
            }
        }
        """.trimIndent()

    private fun requireAndroidSdk() {
        assumeTrue("Android SDK not available for AGP functional tests", androidSdkAvailable())
    }

    private fun androidSdkAvailable(): Boolean {
        val sdkPath = System.getenv("ANDROID_SDK_ROOT") ?: System.getenv("ANDROID_HOME")
        return !sdkPath.isNullOrBlank() && Path.of(sdkPath).exists()
    }

    private companion object {
        private const val AGP_VERSION = "9.2.1"
        private const val KOTLIN_VERSION = "2.4.0"
    }
}
