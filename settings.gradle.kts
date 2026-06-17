rootProject.name= "retrofit-graphql"
include(":annotations")
include(":api")
include(":android-assets")
include(":runtime")
include(":codegen-core")
include(":gradle-plugin")
include(":serialization-gson")
include(":serialization-kotlinx")
include(":library")

if (!System.getenv().containsKey("CI")) {
    include(":app")
}