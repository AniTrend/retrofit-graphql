plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.serialization"
}

dependencies {
    // GraphQLOperationRequest and GraphQLValue are exposed in the codec contract
    api(project(":api"))
}
