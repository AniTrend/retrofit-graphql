rootProject.name= "retrofit-graphql"
include(":library")

if (!System.getenv().containsKey("CI")) {
    include(":app")
}