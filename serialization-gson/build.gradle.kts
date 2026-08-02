plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.serialization.gson"
}

dependencies {
    // GraphQLTransportCodec contract is exposed in public API
    api(project(":serialization-api"))
    // Neutral protocol contracts (GraphQLOperationRequest, GraphQLResponse, ...)
    api(project(":api"))
    // Gson is exposed in the GsonGraphQLTransportCodec public constructor
    api(libs.gson)
    // The legacy GsonGraphQLJson implementation moved to :compat
}
