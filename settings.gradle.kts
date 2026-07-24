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
include(":codegen-core")
include(":serialization-gson")
include(":serialization-kotlinx")
include(":library")

if (!System.getenv().containsKey("CI")) {
    include(":app")
}