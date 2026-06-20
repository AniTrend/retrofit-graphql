import co.anitrend.retrofit.graphql.buildSrc.plugin.components.configureDokkaForJvm
import co.anitrend.retrofit.graphql.buildSrc.plugin.components.configureSpotlessForJvm
import java.util.Properties
import org.gradle.api.tasks.bundling.Jar

plugins {
    kotlin("jvm")
    `maven-publish`
}

configureSpotlessForJvm()
configureDokkaForJvm()

// Read version from root version.properties for publishing.
// projectDir is annotations/, so parent dir is the main project root.
val versionProps = Properties()
file("${projectDir}/../gradle/version.properties").inputStream().use { versionProps.load(it) }
val libVersion = versionProps.getProperty("name")

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(file("src/main/java"), file("src/main/kotlin"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "co.anitrend"
            artifactId = project.name
            version = libVersion
            from(components["java"])
            artifact(sourcesJar)
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

