plugins {
    id("co.anitrend.retrofit.graphql")
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
}

android {
    // Compat namespace is isolated from the historical source packages:
    // all legacy sources keep their original fully qualified names
    // (co.anitrend.retrofit.graphql.model.*, io.github.wax911.library.*, ...).
    namespace = "co.anitrend.retrofit.graphql.compat"
}

dependencies {
    // Neutral contracts re-exported by the legacy surface (GraphQLDocumentRegistry,
    // GraphQLVariables, EmptyGraphQLVariables)
    api(project(":api"))
    // Backend-neutral converters and codec contract referenced by legacy converters
    api(project(":runtime"))
    // Asset discovery, processors, and logger contracts exposed by GraphConverter
    api(project(":android-assets"))
    // @GraphQuery read by GraphRequestConverter and GraphConverter
    api(project(":annotations"))
    // Gson is exposed in GsonGraphQLJson and the legacy factory overloads;
    // kotlinx.serialization backs the @Serializable legacy models and KotlinxGraphQLJson
    api(libs.gson)
    api(libs.jetbrains.kotlinx.serialization.json)

    testImplementation(libs.jetbrains.kotlin.reflect)
    // Robolectric is used only by the Parcelize round-trip compatibility test
    // (android.os.Parcel is not functional against the mockable android.jar).
    testImplementation(libs.robolectric)
}
