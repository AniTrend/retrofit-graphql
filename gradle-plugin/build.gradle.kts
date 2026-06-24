import co.anitrend.retrofit.graphql.buildSrc.plugin.components.applyStandaloneBuildCoordinates
import co.anitrend.retrofit.graphql.buildSrc.plugin.components.configureSpotlessForJvm
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

applyStandaloneBuildCoordinates()

configureSpotlessForJvm()

val sourceSets = the<SourceSetContainer>()
val functionalTestSourceSet = sourceSets.create("functionalTest")

configurations[functionalTestSourceSet.implementationConfigurationName].extendsFrom(
    configurations.testImplementation.get(),
)
configurations[functionalTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(
    configurations.testRuntimeOnly.get(),
)
val functionalTestPluginClasspath = configurations.create("functionalTestPluginClasspath") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

gradlePlugin {
    testSourceSets(functionalTestSourceSet)
    plugins {
        create("retrofitGraphQL") {
            id = "co.anitrend.retrofit.graphql.codegen"
            implementationClass = "co.anitrend.retrofit.graphql.codegen.RetrofitGraphQLPlugin"
        }
    }
}

publishing {
    publications.withType(MavenPublication::class.java).configureEach {
        pom {
            name.set("Retrofit GraphQL Gradle Plugin")
            description.set("Standalone Gradle plugin for retrofit-graphql GraphQL source generation")
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

dependencies {
    api(project(":codegen-core"))
    implementation(libs.kotlinpoet)
    implementation(gradleApi())
    compileOnly(libs.android.gradle.plugin)

    add(functionalTestSourceSet.implementationConfigurationName, gradleTestKit())
    add(functionalTestSourceSet.implementationConfigurationName, kotlin("test"))
    add(functionalTestSourceSet.implementationConfigurationName, libs.junit)
    add(functionalTestSourceSet.runtimeOnlyConfigurationName, libs.android.gradle.plugin)
    add(functionalTestPluginClasspath.name, libs.android.gradle.plugin)
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(functionalTestPluginClasspath)
}

val functionalTest = tasks.register<Test>("functionalTest") {
    description = "Runs Gradle TestKit functional tests"
    group = "verification"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    filter {
        excludeTestsMatching("co.anitrend.retrofit.graphql.codegen.PublishedConsumerPluginResolutionTest")
    }
    shouldRunAfter(tasks.test)
}

val publishedConsumerTest = tasks.register<Test>("publishedConsumerTest") {
    description = "Verifies plugin resolution from mavenLocal"
    group = "verification"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    // Runs separately from functionalTest because it validates the published-marker flow
    // and needs fresh local publications before executing.
    dependsOn("publishToMavenLocal")
    filter {
        includeTestsMatching("co.anitrend.retrofit.graphql.codegen.PublishedConsumerPluginResolutionTest")
    }
    shouldRunAfter(functionalTest)
}

tasks.check {
    dependsOn(functionalTest)
}
