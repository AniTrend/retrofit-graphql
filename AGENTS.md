<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
|------|----------|
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.

---

# Retrofit GraphQL — Project Context

## Overview

**Retrofit GraphQL** is a converter library for Retrofit that enables injection of `.graphql` query/mutation files into HTTP request bodies with GraphQL variables. It bridges Retrofit's HTTP capabilities with GraphQL's query-based approach, letting Android developers work with raw `.graphql` files while keeping full control over their model classes.

### Architecture Principles

- **Converter Pattern** — Core functionality is a Retrofit `Converter.Factory` that transforms annotated method calls into GraphQL HTTP requests.
- **File-Based + Optional Code Generation** — Supports both runtime `.graphql` file loading from assets (via `@GraphQuery` annotation) AND build-time code generation via a Gradle plugin. Developers choose between flexible hand-written models and type-safe generated helpers.

## Module Organization

The project is organized into composable modules under the `co.anitrend.retrofit.graphql` package root:

| Module | Type | Purpose |
|--------|------|---------|
| `:annotations` | Kotlin JVM | `@GraphQuery` annotation (runtime retention) |
| `:api` | Android | Backend-neutral protocol/operation/registry contracts only: `GraphQLOperation`, `GraphQLDocumentRegistry`, `GraphQLVariables`, `EmptyGraphQLVariables`, `GraphQLOperationRequest`, `GraphQLResponse`/`GraphQLData`/`GraphQLResponseError`, `GraphQLValue`, `GraphQLPathSegment`. No Parcelize, no Kotlin serialization, no Gson/Kotlinx, no Android framework types. |
| `:android-assets` | Android | Runtime asset-based query discovery: `GraphProcessor`, `AssetManagerDiscoveryPlugin`, APQ, logging |
| `:runtime` | Android | Backend-neutral Retrofit `Converter.Factory`: `GraphQLConverterFactory`, `GraphQLRequestConverter`, `GraphQLResponseConverter` (codec-backed via `GraphQLTransportCodec`). No Gson/Kotlinx dependencies; no legacy converter sources. |
| `:compat` | Android | **Deprecated** legacy implementation module. Owns the serializer/Android-coupled surface with original FQCNs: `GraphConverter`, `GraphRequestConverter`, `GraphResponseConverter`, `GraphErrorUtil`, `GraphQLRequest`, `GraphQLJson`, `GsonGraphQLJson`, `KotlinxGraphQLJson`, `GraphContainer`, `GraphError`, `QueryContainer(Builder)`, persisted-query URL parameter types, `GraphQLResponseException`/helpers, and all 37 `io.github.wax911.library.*` type aliases. Depends on `:api`, `:runtime`, `:android-assets`, `:annotations`, Gson, and kotlinx.serialization. |
| `:codegen-core` | Kotlin JVM | Code generation engine: parses `.graphql` files with graphql-java, generates Kotlin with KotlinPoet. Uses `SchemaIndex` for typed schema metadata, `GraphQLTypeMapper` for kind-aware mapping, `GraphQLDefaultValueRenderer` for default literals, and `GraphQLTypeUsageValidator` for recursive scalar validation. |
| `:gradle-plugin` | Gradle Plugin | Plugin `id("co.anitrend.retrofit.graphql.codegen")`. Registers `GenerateGraphQLSourcesTask`, wires output into source sets, owns standalone functional tests, and publishes its own marker/implementation artifacts from the composite build. |
| `:serialization-api` | Android | Backend-neutral transport contract: `GraphQLTransportCodec` plus codec exceptions. Depends on `:api` only; no serializer/HTTP/Android framework APIs. |
| `:serialization-gson` | Android | Gson-backed transport codec: `GsonGraphQLTransportCodec` only. The legacy `GsonGraphQLJson` moved to `:compat`. |
| `:serialization-kotlinx` | Android | kotlinx.serialization-backed transport codec: `KotlinxGraphQLTransportCodec` only. The legacy `KotlinxGraphQLJson` moved to `:compat`. |
| `:library` | Android | **Deprecated** aggregate. Re-exports `:compat` plus every module needed by the historical artifact via `api()`. No sources of its own; the `io.github.wax911.library.*` aliases now ship from `:compat`. |
| `:app` | Android | Sample GitHub API client demonstrating the generated-request path (explicit `GraphQLConverterFactory` + `KotlinxGraphQLTransportCodec` + neutral `GraphQLResponse`/`GraphQLData`) and the legacy asset-based bucket flow through `:compat` (`GraphConverter` + `GraphContainer`). Uses generated registry, typed request helpers, and explicit multipart upload. |

### Dependency Graph

```
:app → :runtime → :api + :serialization-api + :android-assets + :annotations (serialization-api for the codec-backed factory; android-assets and annotations pulled transitively via :runtime)
:app → :compat (legacy GraphConverter/GraphContainer/GraphQLRequest surface for the bucket asset-based flow)
:app → :serialization-gson/:serialization-kotlinx (optional codec backends)
:app → :gradle-plugin → :codegen-core (build-time only, generates typed operation helpers)
:compat → :api + :runtime + :android-assets + :annotations + gson + kotlinx-serialization (legacy implementation only)
:serialization-api → :api (neutral codec contract, consumed via :library aggregate)
:serialization-gson/:serialization-kotlinx → :api + :serialization-api + backend (transport codecs only)
:library (deprecated) → api() aggregates :annotations, :api, :android-assets, :runtime, :compat, :serialization-api, :serialization-gson, :serialization-kotlinx
```

**Compatibility guarantee**: every legacy class and alias exists exactly once, at its historical fully qualified name, and is shipped only by `:compat` (reachable through `:library`). `:api`, `:runtime`, `:serialization-api`, and the codec modules never carry a legacy class, so the aggregate classpath has no duplicates. The `io.github.wax911.library.*` names are Kotlin type aliases: each typealias-only source file emits only an empty `*Kt` file-facade class with no members, so no keep rules are needed for them.

## Build Logic & Conventions

- **Shared Gradle plugin**: `co.anitrend.retrofit.graphql` (`buildSrc`) applies Android config, Dokka, Spotless, publishing, and dependency strategies. Avoid duplicating configuration in individual modules.
- **Version catalog**: `gradle/libs.versions.toml`. Add new deps there first, then reference by alias.
- **Toolchain**: Java/Kotlin 21 (pinned in `.java-version`). Local dev uses `jenv`; CI uses `actions/setup-java`.
- **SDK**: `compileSdk=37`, `minSdk=23`, `targetSdk=37`.
- **Formatting**: Ktlint via Spotless. License header at `spotless/copyright.kt`.
- **Publishing**: JitPack. Root modules publish from the main build; the standalone `:gradle-plugin` composite build must also publish separately. `.jitpack.yml` runs both publish commands in a single fail-fast install step using `&&` and `--no-daemon`: `CI=true ./gradlew --no-daemon build publishToMavenLocal && CI=true ./gradlew --no-daemon -p gradle-plugin publishToMavenLocal`. This keeps JitPack from reporting a partial success if the root publication fails, excludes `:app` via `settings.gradle.kts`, and still materializes the plugin marker + implementation artifacts and the included `:codegen-core` dependency. Do not commit Android Studio generated `gradle/gradle-daemon-jvm.properties` unless the CI and JitPack environments are explicitly verified, because JetBrains daemon JVM criteria can force remote toolchain provisioning instead of using JitPack's configured OpenJDK.
- **CI**: `android-test.yml` runs `./gradlew test -PincludeSampleApp=true` on every push/PR to `develop`, so every module including `:app` is built and unit-tested in CI. `:app` is only excluded from the default project set via `settings.gradle.kts` when the `CI` env var is set without `includeSampleApp=true` (used by JitPack). `release-r8-verification.yml` additionally runs `:app:releaseR8Verification` (managed-device release instrumentation) on `develop`.
- **Dokka**: Generated via `buildSrc` `AndroidOptions.kt`; root `build.gradle.kts` aggregates `:annotations`, `:api`, `:android-assets`, `:runtime`, `:compat`, `:codegen-core`, `:serialization-api`, `:serialization-gson`, `:serialization-kotlinx`, and `:library`. Published at `https://anitrend.github.io/retrofit-graphql/`.
- **Codegen DSL**: `serializationBackend` property (`NONE`/`KOTLINX`/`GSON`) on `common {}` and `target {}` blocks controls annotation emission. The configured backend is used as-is: `NONE` stays `NONE` even with `generateResponses=true`, emitting plain response models (including plain sealed structures for abstract types) with no serializer imports, annotations, or adapters.
- **Codegen task**: `GenerateGraphQLSourcesTask` uses `@CacheableTask` + `@PathSensitive(RELATIVE)` + sorted inputs for deterministic build cache behavior.
- **R8**: Sample app enables R8 in release builds (`isMinifyEnabled = true`, `isShrinkResources = true`) with 2 targeted keep rules for Gson upload path. kotlinx.serialization consumer rules are sufficient for generated types.

## Code Style & Documentation

### Kotlin conventions
- Use Ktlint/Spotless for formatting.
- Prefer immutable `val`, data classes, sealed classes, extension functions.
- Packages: lowercase, hierarchical. Classes: PascalCase. Constants: SCREAMING_SNAKE_CASE.

### KDoc & Dokka
- KDoc is **consumer documentation**. The generated Dokka site is the public API reference.
- Document all new/changed public types: classes, interfaces, objects, enums, annotations, functions, properties.
- Write for someone outside this repo. Explain what the API does, when to use it, and how it fits.
- For annotations (`@GraphQuery`): document expected argument format, assets folder path convention, runtime behavior.
- For converters: document Retrofit integration (how to register, ordering, supported request types).
- For discovery plugins: document contract (what to return, lifecycle events, threading assumptions).
- Use `@param`, `@property`, `@return`, `@throws`, `@see`, `@since` tags where they add value.
- Avoid placeholder KDoc that restates the type name. Explain behavior, not just existence.
- `internal` packages are suppressed from Dokka. Keep public API in documented packages.
- Update KDoc in the same patch as behavior changes.

## Testing

- **Unit tests**: JUnit 4 (the JUnit Platform is disabled in `buildSrc`; see the `useJUnitPlatform()` note in `AndroidConfiguration.kt`), MockK for mocking.
- **Focus areas**: annotation processing logic, GraphQL file parsing, variable binding, error handling.
- **Integration tests**: Retrofit integration with sample operations, multipart uploads, error scenarios.
- **Sample app**: Manual testing for new features. Ensure it demonstrates all major capabilities.

## GraphQL File Management

- Sample `.graphql` files in the `:app` module live under `src/main/graphql/`: the GitHub schema plus `queries/`, `mutations/`, `fragments/` folders are the codegen input, and the bucket mutation under `mutations/bucket/` is excluded from codegen and served by the legacy asset-based flow through `:compat`.
- Schema file at `src/main/graphql/schema.graphql` (used by codegen for input object/enum generation).
- Generated output from codegen plugin at `build/generated/source/graphql/`.

## Extension Points

- **Discovery plugins**: Implement the plugin interface for custom file sources (assets, external storage, network).
- **Logger**: Implement logger contracts for custom logging frameworks.
- **Serialization**: For the deprecated `GraphConverter` path, implement `GraphQLJson` (see `:compat`); built-in implementations: `GsonGraphQLJson`, `KotlinxGraphQLJson`. For the backend-neutral transport path, implement `GraphQLTransportCodec` (see `:serialization-api`); built-in implementations: `GsonGraphQLTransportCodec` (`:serialization-gson`), `KotlinxGraphQLTransportCodec` (`:serialization-kotlinx`).

## Scope & Limitations

The library does NOT provide:
- **Schema validation** — No compile-time GraphQL schema validation.
- **Full code generation** — Optional build-time codegen generates operation constants, a document registry, enum classes, variable classes, input object classes, and typed request helpers. It is NOT a full schema-to-Kotlin code generator like Apollo.
- **Caching** — No built-in response caching (relies on HTTP/OkHttp).
- **Subscriptions** — HTTP-based only. No WebSocket or subscription support.
- **Schema introspection** — No runtime schema discovery.

## Context Maintenance

When making changes that alter the repository's reality, update related documentation in the same patch:

- **Module changes** (new modules, renamed modules, changed dependencies): update the Module Organization section above, `.agents/skills/retrofit-graphql-reference-map/references/module-map.md`, and `.agents/skills/retrofit-graphql-build-dependencies/references/build-map.md`.
- **Build convention changes**: update the Build Logic section above and `build-map.md`.
- **Public API changes**: update KDoc and relevant skill references.
- **Consumer-facing changes** (integration patterns, configuration): update `README.md`, `MIGRATION.md`, and wiki pages.
- **Remove or rewrite contradictory guidance** instead of layering new instructions on top of obsolete ones.

## Agent Skills

Repository-specific skills live under `.agents/skills/`:

| Skill | Purpose |
|-------|---------|
| `jenv-gradle-low-ram` | JDK alignment with `.java-version` and low-RAM Gradle invocation |
| `retrofit-graphql-build-dependencies` | Build map, module dependencies, and CI concerns |
| `retrofit-graphql-kdoc-dokka` | KDoc checklist and Dokka generation workflow |
| `retrofit-graphql-reference-map` | Module-to-package map, consumer entry points, placement heuristics |
