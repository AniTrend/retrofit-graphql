plugins {
    id("co.anitrend.retrofit.graphql")
    kotlin("plugin.serialization")
}

android {
    namespace = "co.anitrend.retrofit.graphql.serialization.kotlinx"
}

dependencies {
    // GraphQLTransportCodec contract is exposed in public API
    api(project(":serialization-api"))
    // Neutral protocol contracts (GraphQLOperationRequest, GraphQLResponse, ...)
    api(project(":api"))
    // kotlinx.serialization.json.Json is exposed in the KotlinxGraphQLTransportCodec public constructor
    api(libs.jetbrains.kotlinx.serialization.json)
    // The legacy KotlinxGraphQLJson implementation moved to :compat
}
