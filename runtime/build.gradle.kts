plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.runtime"
}

dependencies {
    // Exposed in public API: GraphQLDocumentRegistry, GraphQLOperationRequest
    api(project(":api"))
    // Exposed in public API: GraphQLTransportCodec (GraphQLConverterFactory and converters)
    api(project(":serialization-api"))
    // Discovery/logger contracts historically exposed through :runtime remain
    // transitively available; the legacy GraphConverter that consumed them
    // moved to :compat (which re-declares android-assets itself).
    api(project(":android-assets"))
    // @GraphQuery read via reflection by the legacy converters in :compat
    implementation(project(":annotations"))
    // No Gson/kotlinx.serialization dependency: the codec-backed
    // GraphQLConverterFactory path is backend-neutral. The legacy
    // Gson/GraphQLJson surface moved to :compat.
}
