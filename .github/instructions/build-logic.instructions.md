---
description: Use when editing Gradle files, module dependencies, Dokka configuration, version catalog entries, GitHub workflows, or buildSrc logic in retrofit-graphql.
applyTo: build.gradle.kts, settings.gradle.kts, gradle/**/*.toml, buildSrc/**/*.kt, */build.gradle.kts, .github/workflows/*.yml
---

# Build Logic Guidance

- Prefer the shared `co.anitrend.retrofit.graphql` Gradle plugin and `buildSrc` helpers over duplicating Android, Kotlin, Dokka, Spotless, publishing, or test configuration in individual modules.
- The pinned Java and Kotlin toolchain is 21. Keep new build logic compatible with `.java-version` (`21.0.8`) and the shared Android configuration. The `.java-version` file is read by `jenv` on local developer machines for automatic Java version selection; CI uses `actions/setup-java` instead.
- Add or update dependency versions in `gradle/libs.versions.toml` first, then reference the alias from modules or build logic.
- The `:app` module is excluded from the Gradle build when the `CI` environment variable is set (see `settings.gradle.kts`). Only `:library` is built and tested in CI.
- Shared Android defaults come from `buildSrc`, including `compileSdk = 35`, `minSdk = 23`, `targetSdk = 35`, JUnit Platform, Kotlin toolchain 21, and packaging exclusions.
- Shared formatting comes from `buildSrc/...AndroidConfiguration.kt` (Spotless configuration) and the license header file under `spotless/copyright.kt`.
- Shared documentation behavior comes from `buildSrc/...AndroidOptions.kt` (Dokka task configuration) and `.github/workflows/gradle-dokka.yml`.
- If you need a new convention across modules, prefer adding it once in `buildSrc` instead of repeating it in each `build.gradle.kts`.
- When validating Gradle changes locally, pair the work with the existing `jenv-gradle-low-ram` skill if JDK alignment or memory pressure becomes a problem.
