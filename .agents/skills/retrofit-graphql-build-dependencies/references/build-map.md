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
| Publishing options | `buildSrc/.../components/AndroidOptions.kt` (Android), per-module `build.gradle.kts` (JVM) | AGP component-backed publications via `singleVariant("release")` + `withSourcesJar()`. JVM modules (`:annotations`, `:codegen-core`) inline publishing. Publication name `"maven"` for all modules. |
| Dependency versions and aliases | `gradle/libs.versions.toml` | Add or update aliases here first |
| Library build | `library/build.gradle.kts` | Applies the shared plugin; no module-specific overrides needed for standard changes |
| Sample app build | `app/build.gradle.kts` | Applies the shared plugin; Room compiler options, build config fields from `.config/` |
| Dokka publication | `.github/workflows/gradle-dokka.yml` | Runs on `develop`, generates `library/build/docs/dokka`, deploys to `docs` branch |

## Module Dependency Snapshot

- library: **deprecated** aggregator module that transitively re-exports all library sub-modules via api() dependencies (annotations, api, android-assets, runtime, serialization-gson, serialization-kotlinx).
- app: sample application that depends on :runtime and :api only (no direct :android-assets or :annotations deps). Uses the codegen Gradle plugin for build-time generated types. :android-assets and :annotations are pulled transitively via :runtime.

## Edit Strategy

- New library version or alias: `libs.versions.toml`.
- Cross-module convention: `buildSrc`.
- One module only: that module's `build.gradle.kts`.
- Documentation generation or publish behavior: Dokka config in `buildSrc/...AndroidOptions.kt` plus the workflow file.
- Memory or JVM tuning: `gradle.properties` (repo-wide) or `~/.gradle/gradle.properties` (user-local, preferred for personal overrides).
