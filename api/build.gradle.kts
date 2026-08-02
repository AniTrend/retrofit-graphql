plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql.api"
}

dependencies {
    // :api contains only backend-neutral protocol/operation/registry contracts
    // and standard Kotlin/Java types. It must not apply Parcelize or Kotlin
    // serialization, depend on Gson/Kotlinx, or expose Android framework types.
    // The legacy serializer/Android-coupled models moved to :compat.
}
