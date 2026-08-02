pluginManagement {
    includeBuild("gradle-plugin")
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name= "retrofit-graphql"
include(":annotations")
include(":api")
include(":android-assets")
include(":runtime")
include(":compat")
include(":codegen-core")
include(":serialization-api")
include(":serialization-gson")
include(":serialization-kotlinx")
include(":library")

val includeSampleApp = providers.gradleProperty("includeSampleApp")
    .map(String::toBoolean)
    .orElse(false)
    .get()

if (!System.getenv().containsKey("CI") || includeSampleApp) {
    include(":app")
}
