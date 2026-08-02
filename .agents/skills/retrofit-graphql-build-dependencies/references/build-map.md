# Build Map

Use this map to choose the right build file before editing.

| Concern | Primary files | Notes |
| --- | --- | --- |
| Module includes | `settings.gradle.kts` | Includes :compat and :library always; includes :app only when CI env var is not set or `includeSampleApp=true` is passed |
| Root build configuration | `build.gradle.kts` | Top-level plugin declarations, repository configuration, and multi-module Dokka aggregation |
| Shared plugin entry point | `buildSrc/.../plugin/CorePlugin.kt` | Applies Android plugin, Dokka, Spotless, publishing, sources, and dependencies based on module type |
| Shared Android defaults | `buildSrc/.../components/AndroidConfiguration.kt` | SDK levels (`compileSdk=37`, `minSdk=23`, `targetSdk=37`), JUnit 4 (the JUnit Platform is disabled; see the `useJUnitPlatform()` note), Kotlin toolchain 21, packaging exclusions, Spotless |
| Shared dependency strategy | `buildSrc/.../strategy/DependencyStrategy.kt` | Default Kotlin and test libraries per module type; Retrofit/OkHttp as `api` for :runtime; Gson + converter-gson added for :compat only; Gson/Retrofit/OkHttp as `implementation` for :app |
| Shared Dokka behavior | `buildSrc/.../components/AndroidOptions.kt` | `dokkaHtml` task, `reportUndocumented = true`, internal packages suppressed, Android docs linked |
| Shared formatting | `buildSrc/.../components/AndroidConfiguration.kt` (Spotless config), `spotless/copyright.kt` | Ktlint and license header configuration |
| Publishing options | `buildSrc/.../components/AndroidOptions.kt` (Android), per-module `build.gradle.kts` (JVM), `gradle-plugin/build.gradle.kts`, `gradle-plugin/buildSrc/.../JvmConfiguration.kt`, `.jitpack.yml` | Root Android publications use AGP components. The standalone `gradle-plugin` composite build publishes both the plugin implementation + marker artifacts and republishes included `:codegen-core` with explicit coordinates from `gradle/version.properties`. `.jitpack.yml` must run both root and standalone plugin `publishToMavenLocal` commands with `CI=true` so JitPack excludes `:app` via `settings.gradle.kts`. Keep the commands in one `&&`-chained install step so root publication failures fail the build instead of publishing only plugin artifacts. Use `--no-daemon` on JitPack and avoid committing Android Studio generated `gradle/gradle-daemon-jvm.properties` unless CI/JitPack toolchain provisioning has been verified. |
| Dependency versions and aliases | `gradle/libs.versions.toml` | Add or update aliases here first. Robolectric lives here too (test-only, used by the `:compat` Parcelize round-trip test) |
| Compat module build | `compat/build.gradle.kts` | Applies the shared plugin plus `kotlin-parcelize` and `kotlin("plugin.serialization")`; `api()` deps on `:api`, `:runtime`, `:android-assets`, `:annotations`, Gson, kotlinx-serialization-json |
| Library build | `library/build.gradle.kts` | Deprecated aggregate; `api()` re-exports all modules including `:compat`. No sources of its own. The old `consumer-rules.pro` (`-keep io.github.wax911.library.model.**`) was removed: the type aliases emit only empty `*Kt` file-facade classes, so the rule was behaviorally inert |
| Sample app build | `app/build.gradle.kts` | Applies the shared plugin; codegen DSL (`serializationBackend`, `generateResponses`, scalar mappings), R8 enabled (`isMinifyEnabled = true`, `isShrinkResources = true`), Room compiler options, build config fields from `.config/`. Depends on `:runtime`, `:api`, `:compat`, `:serialization-kotlinx` |
| R8 keep rules | `app/proguard-rules.pro` | 2 targeted keep rules for the Gson upload path (`GraphQLRequest` from `:compat`, `UploadToStorageBucketVariables`). kotlinx.serialization consumer rules protect generated types automatically. The neutral `GraphQLOperationRequest` is deliberately not kept: `verifyReleaseMapping` asserts R8 renames it |
| Backend-neutrality check | `BackendDependencyBoundaryTest` in `:api`, `:serialization-api`, `:runtime` test sources + `CompatOwnsLegacyBackendTest` in `:compat` | Classpath-scan unit tests running on the resolved test runtime classpath: the neutral modules fail when Gson/kotlinx artifacts appear, `:compat` fails when they are absent |
| Dokka publication | `.github/workflows/gradle-dokka.yml` | Runs on `develop`, generates aggregate docs, deploys to `docs` branch |

## Module Dependency Snapshot

- library: **deprecated** aggregator module that transitively re-exports all library sub-modules via api() dependencies (annotations, api, android-assets, runtime, compat, serialization-api, serialization-gson, serialization-kotlinx). No main or test sources beyond the aggregate classpath duplicate test.
- api: Android library module registered in `Modules.Components` + `isLibraryModule()`; depends on nothing (no Gson, no kotlinx, no Parcelize, no serialization plugin). Backend-neutral contracts only.
- runtime: Android library module registered in `Modules.Components` + `isLibraryModule()`; depends on `:api`, `:serialization-api`, `:android-assets` via `api()` and `:annotations` via `implementation()`. Retrofit/OkHttp arrive via `DependencyStrategy.applyRetrofitDependencies()` (no Gson, no converter-gson). The legacy `GraphConverter`/`GraphRequestConverter`/`GraphResponseConverter`/`GraphErrorUtil` moved to `:compat`; `:serialization-gson` was dropped entirely.
- compat: Android library module registered in `Modules.Components` + `isLibraryModule()`; the only deprecated legacy implementation module. Depends on `:api`, `:runtime`, `:android-assets`, `:annotations` via `api()`, plus Gson and kotlinx-serialization-json via `api()` (both exposed in the legacy public API). Applies `kotlin-parcelize` (QueryContainer/QueryContainerBuilder) and `kotlin("plugin.serialization")` (@Serializable legacy models). Retrofit/OkHttp/Gson/converter-gson arrive via the shared strategy. Publishes with its own artifact id (`compat`).
- serialization-api: Android library module registered in `Modules.Components` + `isLibraryModule()`; depends on `:api` via `api()` only, no serializer/HTTP/Android framework APIs in its sources. Included in root Dokka aggregation (`build.gradle.kts`) and `:library` so CI reaches it.
- serialization-gson / serialization-kotlinx: Android library modules depending on `:api` + `:serialization-api` via `api()`; each additionally exposes its backend (`gson` / kotlinx-serialization-json) via `api()` because the public `*GraphQLTransportCodec` constructor exposes backend types. The legacy `GsonGraphQLJson`/`KotlinxGraphQLJson` implementations moved to `:compat`, so these modules ship transport codecs only.
- app: sample application that depends on :runtime, :api, :compat, and :serialization-kotlinx. Uses the codegen Gradle plugin for build-time generated types with KOTLINX serialization backend. GitHub endpoints use `GraphQLConverterFactory` + `KotlinxGraphQLTransportCodec` + `GraphQLResponse`; the bucket endpoint keeps the legacy `GraphConverter` + `GraphContainer` flow through `:compat`. R8 enabled in release builds with 2 targeted keep rules for the Gson upload path.

## Edit Strategy

- New library version or alias: `libs.versions.toml`.
- Cross-module convention: `buildSrc`.
- One module only: that module's `build.gradle.kts`.
- Documentation generation or publish behavior: Dokka config in `buildSrc/...AndroidOptions.kt` plus the workflow file.
- Standalone plugin publishing or plugin marker issues: `gradle-plugin/build.gradle.kts`, `gradle-plugin/settings.gradle.kts`, `gradle-plugin/buildSrc/.../JvmConfiguration.kt`, and `.jitpack.yml`.
- Memory or JVM tuning: `gradle.properties` (repo-wide) or `~/.gradle/gradle.properties` (user-local, preferred for personal overrides). Avoid committing generated daemon JVM criteria unless shared CI runners have been verified with those criteria.
