plugins {
    id("co.anitrend.retrofit.graphql")
}

android {
    namespace = "co.anitrend.retrofit.graphql"
}

/**
 * :library is the deprecated aggregate facade that reproduces the historical
 * `co.anitrend:retrofit-graphql` artifact. It re-exports every module the
 * historical artifact shipped: the legacy implementation surface now lives in
 * `:compat` (including the `io.github.wax911.library.*` type aliases), while
 * the neutral contracts, converters, and built-in transport codecs stay
 * exposed directly for consumers that reference them by FQCN.
 *
 * Consumer rules: the previous `consumer-rules.pro` kept
 * `io.github.wax911.library.model.**`. That rule was behaviorally inert: the
 * type aliases themselves produce no classes, and the only runtime classes
 * under `io.github.wax911.library.*` are the empty `*Kt` file-facade classes
 * Kotlin emits for typealias-only source files (no members, nothing for R8 to
 * keep or rename). The aliases now live in `:compat` with the same shape, so
 * no keep rule is needed for them; do not reintroduce broad keep rules.
 */
dependencies {
    api(project(":annotations"))
    api(project(":api"))
    api(project(":android-assets"))
    api(project(":runtime"))
    api(project(":compat"))
    api(project(":serialization-api"))
    api(project(":serialization-gson"))
    api(project(":serialization-kotlinx"))

    testImplementation(libs.jetbrains.kotlin.reflect)
}
