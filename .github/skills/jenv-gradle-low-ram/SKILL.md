---
name: jenv-gradle-low-ram
description: 'Align jenv with .java-version and run Gradle reliably on low-RAM machines. Use for Java mismatch errors, Gradle OOMs, daemon memory pressure, and selecting safe build flags.'
argument-hint: 'Describe your target task and available RAM, for example: assembleDebug on 8GB RAM'
---
# Jenv + Gradle Low-RAM Workflow

## What This Skill Produces

- A shell where `java` and Gradle use the Java version pinned by `.java-version`.
- A repeatable Gradle command profile tuned for constrained memory.
- Verification checks to confirm version alignment and build stability.

## When To Use

- Any time `./gradlew` is about to be invoked in this repository.
- Especially important for low-RAM machines where daemon/worker pressure can destabilize builds.
- Required when verifying that Java from `.java-version` is the active runtime for Gradle.

## Context

This repository pins Java `21.0.8` in `.java-version`. On developer machines with `jenv` installed, navigating to the project root automatically selects that JDK. In CI, `actions/setup-java` with `java-version: 21` handles selection instead. The `copilot-setup-steps.yml` workflow mirrors the CI approach.

## Procedure

1. Detect the required Java version.

```bash
cat .java-version
```

2. Verify `jenv` is installed and initialized.

```bash
command -v jenv
jenv versions
jenv version
```

Decision point:

- If `jenv` is not found, install and initialize it in your shell startup.
- If `jenv` is found but `jenv version` does not match `.java-version`, continue to step 3.

3. Align local Java to the repository pin.

```bash
required_java="$(cat .java-version)"
jenv local "$required_java"
```

Decision point:

- If `jenv local` fails because the version is missing, install that JDK and run `jenv add <jdk-path>`, then retry.

4. Confirm runtime alignment before any Gradle task.

```bash
java -version
./gradlew -version
```

Quality check:

- Gradle JVM version in `./gradlew -version` must match the `.java-version` major version (21).

5. Run Gradle with low-memory-safe defaults.

Preferred one-off profile for a debug build (`:app` module; not available in CI):

```bash
./gradlew --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8" \
  :app:assembleDebug
```

For library unit tests:

```bash
./gradlew --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs="-Xmx1792m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8" \
  :library:test
```

For Spotless formatting check:

```bash
./gradlew --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8" \
  spotlessCheck
```

6. If memory pressure continues, downgrade concurrency.

```bash
./gradlew --no-daemon --max-workers=1 -Dorg.gradle.parallel=false :library:test
```

7. If builds are stable and you want persistence, prefer user-local Gradle overrides (avoid committing repo-wide memory changes).

Suggested entries for `~/.gradle/gradle.properties`:

```properties
org.gradle.daemon=false
org.gradle.parallel=false
org.gradle.workers.max=2
org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
```

## Troubleshooting Branches

- `jenv version` correct but `java -version` wrong:
  Ensure shell init runs `eval "$(jenv init -)"` and enable export plugin with `jenv enable-plugin export`.
- `Gradle daemon disappeared unexpectedly`:
  Retry with `--no-daemon --max-workers=1` and lower `-Xmx` if the OS is reclaiming memory aggressively.
- Kotlin compile OOM:
  Keep workers low, disable parallel, and run module-targeted tasks (`:library:test` or `:app:assembleDebug`) instead of full-project builds.
- `:app` not found:
  The app module is excluded when the `CI` environment variable is set. Run debug builds locally without `CI` set, or unset it temporarily.

## Completion Checks

- `.java-version` and `jenv version` resolve to the same Java release.
- `./gradlew -version` reports the expected JVM.
- Target Gradle task completes without OOM or daemon crash.
- Machine remains responsive during build (no prolonged swap thrash).

## Fast Invocation Examples

- "Align my shell to `.java-version` and run debug build with 8GB RAM settings"
- "I get class file major version errors; fix jenv and verify Gradle JVM"
- "Run library unit tests with a low-memory Gradle profile and fallback if OOM occurs"
