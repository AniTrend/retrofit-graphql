# Build Map

Use this map to choose the right build file before editing.

| Concern | Primary files | Notes |
| --- | --- | --- |
| Module includes | `settings.gradle.kts` | Includes :library always (aggregates all sub-modules); includes :app only when CI env var is not set |
| Root build configuration | `build.gradle.kts` | Top-level plugin declarations and repository configuration |
| Shared plugin entry point | `buildSrc/.../plugin/CorePlugin.kt` | Applies Android plugin, Dokka, Spotless, publishing, sources, and dependencies based on module type |
| Shared Android defaults | `buildSrc/.../components/AndroidConfiguration.kt` | SDK levels (`compileSdk=37`, `minSdk=23`, `targetSdk=37`), JUnit Platform, Kotlin toolchain 21, packaging exclusions, Spotless |
| Shared dependency strategy | `buildSrc/.../strategy/DependencyStrategy.kt` | Default Kotlin, Retrofit, OkHttp, coroutines, and test libraries per module type |
| Shared Dokka behavior | `buildSrc/.../components/AndroidOptions.kt` | `dokkaHtml` task, `reportUndocumented = true`, internal packages suppressed, Android docs linked |
| Shared formatting | `buildSrc/.../components/AndroidConfiguration.kt` (Spotless config), `spotless/copyright.kt` | Ktlint and license header configuration |
| Publishing options | `buildSrc/.../components/AndroidOptions.kt` (Android), per-module `build.gradle.kts` (JVM), `gradle-plugin/build.gradle.kts`, `gradle-plugin/buildSrc/.../JvmConfiguration.kt`, `.jitpack.yml` | Root Android publications use AGP components. The standalone `gradle-plugin` composite build publishes both the plugin implementation + marker artifacts and republishes included `:codegen-core` with explicit coordinates from `gradle/version.properties`. `.jitpack.yml` must run both root and standalone plugin `publishToMavenLocal` commands with `CI=true` so JitPack excludes `:app` via `settings.gradle.kts`. |
| Dependency versions and aliases | `gradle/libs.versions.toml` | Add or update aliases here first |
| Library build | `library/build.gradle.kts` | Applies the shared plugin; no module-specific overrides needed for standard changes |
| Sample app build | `app/build.gradle.kts` | Applies the shared plugin; codegen DSL (`serializationBackend`, `generateResponses`, scalar mappings), R8 enabled (`isMinifyEnabled = true`, `isShrinkResources = true`), Room compiler options, build config fields from `.config/` |
| R8 keep rules | `app/proguard-rules.pro` | 2 targeted keep rules for Gson upload path (`GraphQLRequest`, `UploadToStorageBucketVariables`). kotlinx.serialization consumer rules protect generated types automatically. |
| Dokka publication | `.github/workflows/gradle-dokka.yml` | Runs on `develop`, generates `library/build/docs/dokka`, deploys to `docs` branch |

## Module Dependency Snapshot

- library: **deprecated** aggregator module that transitively re-exports all library sub-modules via api() dependencies (annotations, api, android-assets, runtime, serialization-gson, serialization-kotlinx).
- app: sample application that depends on :runtime, :api, and :serialization-kotlinx (no direct :android-assets or :annotations deps). Uses the codegen Gradle plugin for build-time generated types with KOTLINX serialization backend. R8 enabled in release builds with 2 targeted keep rules for Gson upload path. :android-assets and :annotations are pulled transitively via :runtime.

## Edit Strategy

- New library version or alias: `libs.versions.toml`.
- Cross-module convention: `buildSrc`.
- One module only: that module's `build.gradle.kts`.
- Documentation generation or publish behavior: Dokka config in `buildSrc/...AndroidOptions.kt` plus the workflow file.
- Standalone plugin publishing or plugin marker issues: `gradle-plugin/build.gradle.kts`, `gradle-plugin/settings.gradle.kts`, `gradle-plugin/buildSrc/.../JvmConfiguration.kt`, and `.jitpack.yml`.
- Memory or JVM tuning: `gradle.properties` (repo-wide) or `~/.gradle/gradle.properties` (user-local, preferred for personal overrides).
