rootProject.name= "retrofit-graphql"
include(":annotations")
include(":api")
include(":android-assets")
include(":runtime")
include(":library")

if (!System.getenv().containsKey("CI")) {
    include(":app")
}