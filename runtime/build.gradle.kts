plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.runtime"
}

dependencies {
    // Exposed in public API: GraphQLDocumentRegistry, QueryContainerBuilder, GraphQLRequest
    api(project(":api"))
    // Exposed in public API: AbstractGraphProcessor, ILogger, DefaultGraphLogger
    api(project(":android-assets"))
    // @GraphQuery only read via reflection internally, never leaked
    implementation(project(":annotations"))
}
