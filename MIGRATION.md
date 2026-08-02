# Migration Guide

This guide covers migrating consumer projects from the deprecated `:library` aggregator to the new modular architecture, from v2.x Gson-based workflows to the v0.13.x `GraphQLJson` abstraction, and from the serializer-coupled surface to the backend-neutral `GraphQLOperationRequest`/`GraphQLResponse`/`GraphQLTransportCodec` contracts.

## Table of Contents

- [Migration to Modular Dependencies](#migration-to-modular-dependencies) (v2.x)
- [v0.13.x Serialization Contract Migration](#v013x-serialization-contract-migration)
  - [Gson-to-GraphQLJson](#gson-to-graphqljson)
  - [Constructor rename (binary breaking)](#constructor-rename-binary-breaking)
  - [kotlinx parameterized type handling](#kotlinx-parameterized-type-handling)
  - [@Serializable on API types](#serializable-on-api-types)
  - [@Transient fields](#transient-fields)
  - [APQ behavior (kotlinx)](#apq-behavior-kotlinx)
  - [QueryContainerBuilder limitation (kotlinx)](#querycontainerbuilder-limitation-kotlinx)
- [Backend-Neutral Contract Migration](#backend-neutral-contract-migration)
  - [Compatibility guarantee](#compatibility-guarantee)
  - [New codegen request path](#new-codegen-request-path)
  - [Legacy `GraphConverter` stays available](#legacy-graphconverter-stays-available)

---

# Migration to Modular Dependencies

retrofit-graphql has been restructured from a monolithic `:library` module into a set of focused, composable modules. This guide covers migrating consumer projects from the deprecated `:library` aggregator to the new modular architecture.

## Why Migrate?

The `:library` module remains available as a backward-compatible facade, but it will eventually be removed. Benefits of migrating to direct module dependencies:

- **Smaller footprint** — depend only on the modules you actually use
- **Clearer API boundaries** — each module has a well-defined purpose
- **Better build times** — Gradle can parallelize module compilation
- **Opt-in code generation** — new build-time codegen plugin replaces asset-based discovery

## Module Map

| Module | Type | Purpose | Depends On |
|--------|------|---------|------------|
| `:annotations` | JVM | `@GraphQuery` annotation | (none) |
| `:api` | Android | Backend-neutral protocol/operation/registry contracts (`GraphQLOperation`, `GraphQLDocumentRegistry`, `GraphQLVariables`, `EmptyGraphQLVariables`, `GraphQLOperationRequest`, `GraphQLResponse`) | (none) |
| `:android-assets` | Android | Runtime asset-based query discovery (`GraphProcessor`, `AssetManagerDiscoveryPlugin`, APQ) | `:annotations` |
| `:runtime` | Android | Backend-neutral Retrofit converter factory (`GraphQLConverterFactory`, `GraphQLRequestConverter`, `GraphQLResponseConverter`) | `:api`, `:serialization-api`, `:android-assets`, `:annotations` |
| `:compat` | Android | **Deprecated** legacy implementation (`GraphConverter`, `GraphRequestConverter`, `GraphResponseConverter`, `GraphErrorUtil`, `GraphQLRequest`, `GraphQLJson`, `GsonGraphQLJson`, `KotlinxGraphQLJson`, `GraphContainer`, `GraphError`, `QueryContainer(Builder)`, persisted-query URL types, `GraphQLResponseException`/helpers, and the 37 `io.github.wax911.library.*` type aliases) | `:api`, `:runtime`, `:android-assets`, `:annotations`, Gson, kotlinx.serialization |
| `:codegen-core` | JVM | Build-time code generation engine (parses `.graphql`, generates Kotlin) | (external: graphql-java, KotlinPoet) |
| `:gradle-plugin` | Gradle | Plugin (`id("co.anitrend.retrofit.graphql.codegen")`) that runs codegen as a Gradle task | `:codegen-core` |
| `:serialization-api` | Android | Backend-neutral transport contract (`GraphQLTransportCodec` + codec exceptions) | `:api` |
| `:serialization-gson` | Android | Gson-backed `GraphQLTransportCodec` (`GsonGraphQLTransportCodec`) | `:api`, `:serialization-api`, Gson |
| `:serialization-kotlinx` | Android | kotlinx.serialization-backed `GraphQLTransportCodec` (`KotlinxGraphQLTransportCodec`) | `:api`, `:serialization-api`, kotlinx |
| `:library` | Android | **Deprecated** — backward-compatible aggregator (transitively re-exports all modules via `api()`, including `:compat`) | All of the above |

## Migration by Use Case

### Asset-Based Queries (Traditional)

If you use `.graphql` files in `assets/` with `@GraphQuery` annotations and the `GraphProcessor`:

**Before (aggregator):**
```kotlin
// JitPack external:
dependencies {
    implementation("com.github.AniTrend:retrofit-graphql:{tag}")
}

// Internal development:
dependencies {
    implementation(project(":library"))
}
```

**After (individual modules via JitPack -- recommended for consumers):**
```kotlin
dependencies {
    implementation("com.github.AniTrend.retrofit-graphql:runtime:{tag}")
    implementation("com.github.AniTrend.retrofit-graphql:api:{tag}")
    implementation("com.github.AniTrend.retrofit-graphql:android-assets:{tag}")
    implementation("com.github.AniTrend.retrofit-graphql:annotations:{tag}")
    // Only when you still use the deprecated GraphConverter/GraphContainer/
    // GraphQLRequest/QueryContainerBuilder surface (asset-based flows):
    implementation("com.github.AniTrend.retrofit-graphql:compat:{tag}")
}
```

> **Note:** `:runtime` still publishes `:android-assets` with `api` scope, so those types are available transitively. The legacy `GraphConverter` and its models are NOT in `:runtime` anymore: they moved to `:compat`. Add `:compat` whenever your code imports `GraphConverter`, `GraphContainer`, `GraphError`, `GraphQLRequest`, `GraphQLJson`, `QueryContainerBuilder`, `PersistedQuery*`, or any `io.github.wax911.library.*` alias. The `:library` aggregator still re-exports everything, so consumers that keep `:library` do not need to change anything.

**After (internal development with project references):**
```kotlin
dependencies {
    implementation(project(":runtime"))       // GraphQLConverterFactory (backend-neutral)
    implementation(project(":api"))            // GraphQLOperationRequest, GraphQLResponse, GraphQLDocumentRegistry
    implementation(project(":android-assets")) // GraphProcessor, AssetManagerDiscoveryPlugin
    implementation(project(":annotations"))    // @GraphQuery
    implementation(project(":compat"))         // deprecated GraphConverter/GraphContainer surface (legacy flows only)
    implementation(project(":serialization-kotlinx")) // or :serialization-gson; explicit codec for the new path
}
```

### Build-Time Code Generation (New)

If you want to replace asset-based discovery with build-time code generation:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven(url = uri("https://jitpack.io"))
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

// build.gradle.kts
plugins {
    id("co.anitrend.retrofit.graphql.codegen") version "{tag}"
}

dependencies {
    implementation("com.github.AniTrend.retrofit-graphql:runtime:{tag}")
    implementation("com.github.AniTrend.retrofit-graphql:api:{tag}")
    // :annotations and :android-assets are NOT required for codegen-only consumers
    // (they are pulled transitively via :runtime)
}

retrofitGraphQL {
    common {
        generateVariables.set(true)
        generateResponses.set(true)  // opt-in response model generation
    }
    packageName.set("your.package.generated")
    schema.set(file("src/main/graphql/schema.graphql"))
    operations.from(fileTree("src/main/graphql") {
        include("**/*.graphql")
    })
    scalars {
        map("DateTime", "kotlin.String")
        map("GitObjectID", "kotlin.String")
        map("URI", "kotlin.String")
        // Map any custom schema scalars to Kotlin types.
        // Scalars absent from the schema (e.g. "Upload") can also be
        // mapped — they are allowed but generate no type.
    }
}
```

> **Note:** Code generation removes the need to declare `android-assets` directly. `:runtime` still brings it in transitively today, which is a module-boundary detail, not a signal that codegen still relies on asset discovery.
> **Note:** The `:annotations` dependency (for `@GraphQuery`) is also not required for codegen-only consumers. It remains useful for projects that use a mix of asset-based and codegen workflows.

Place your `.graphql` files in `src/main/graphql/` instead of `assets/graphql/`.

Generated output (under `build/generated/source/graphql/`) includes:
- `GeneratedGraphQLRegistry` — build-time operation registry implementing `GraphQLDocumentRegistry`
- `GraphQLOperations`, `GraphQLDocuments`, `GraphQLHashes` — operation constants
- Enum classes — GraphQL enum types generated as Kotlin enum classes
- Variable classes — typed data classes implementing `GraphQLVariables` (when `generateVariables = true`)
- Input object classes — typed data classes for GraphQL input types (when `generateVariables = true`)
- Request helpers — `.request(...)` factory methods on operation objects returning the backend-neutral `GraphQLOperationRequest<VariableType>` (when `generateVariables = true`)
- Response model classes — `{OperationName}Data` classes with nested types for selected fields (when `generateResponses = true`; serialization annotations are emitted per the configured `serializationBackend`)

Wire the registry into the backend-neutral factory via Koin or manual construction (the codec is explicit; the registry is retained for API parity and is never consulted while the neutral request carries its document):

```kotlin
// Koin, no Context required
single<GraphQLDocumentRegistry> { GeneratedGraphQLRegistry }
factory {
    GraphQLConverterFactory.create(
        codec = KotlinxGraphQLTransportCodec(),   // or GsonGraphQLTransportCodec()
        registry = get(),
    )
}

// Manual, registry-only
val registryOnlyFactory = GraphQLConverterFactory.create(
    codec = KotlinxGraphQLTransportCodec(),
    registry = GeneratedGraphQLRegistry,
)
```

Legacy `GraphConverter` (deprecated, `:compat`) remains available for asset-based or mixed migrations:

```kotlin
// Legacy (deprecated, :compat): registry-only, no Context required
single<GraphQLDocumentRegistry> { GeneratedGraphQLRegistry }
factory { GraphConverter.create(registry = get()) }

// Legacy (deprecated, :compat): mixed asset + codegen fallback
factory { GraphConverter.create(context = get(), registry = get()) }

// Legacy (deprecated, :compat): custom Gson + registry-only
val customGsonFactory = GraphConverter.create(
    gson = GsonBuilder().serializeNulls().create(),
    registry = GeneratedGraphQLRegistry,
)
```

> **Note (legacy path):** In registry-only mode, if a `@GraphQuery`-annotated operation is missing from the registry, `GraphConverter` throws `IllegalStateException`. Non-annotated operations (codegen-only consumers using `GraphQLOperationRequest<T>`) proceed without a query lookup because the request already contains the document string.

### Multipart Uploads

For file uploads, register a `RequestBodyPassThroughConverterFactory` before the converter so that `MultipartBody` and `RequestBody` instances bypass GraphQL conversion. The sample app keeps the bucket upload on the legacy `GraphConverter` (`:compat`) because the upload mutation uses the separate bucket schema:

```kotlin
// Legacy (deprecated, :compat) bucket endpoint
val retrofit = Retrofit.Builder()
    .addConverterFactory(RequestBodyPassThroughConverterFactory())
    .addConverterFactory(GraphConverter.create(context, registry = GeneratedGraphQLRegistry))
    .baseUrl(baseUrl)
    .build()
```

Use generated operation metadata for the upload mutation:
```kotlin
val request = UploadToStorageBucket.request(upload = filePath)
// Build MultipartBody from the generated request fields
```

See the [sample app](app/) for a complete migration example (GitHub endpoints use `GraphQLConverterFactory` + `KotlinxGraphQLTransportCodec`; the bucket endpoint keeps the legacy flow).

### Scalar Mappings

Custom GraphQL scalar types used in generated code must be mapped to Kotlin types. Add a `scalars` block to the `retrofitGraphQL` DSL:

```kotlin
retrofitGraphQL {
    packageName.set("your.package.generated")
    schema.set(file("src/main/graphql/schema.graphql"))
    scalars {
        map("DateTime", "kotlin.String")
        map("GitObjectID", "kotlin.String")
        map("URI", "kotlin.String")
    }
    // ...
}
```

Scalars that appear in the schema but are not mapped will cause a build error with a path-aware message indicating where the unmapped scalar was encountered. Scalars that are mapped but absent from the schema (e.g. `"Upload"`) are allowed — they generate no type but do not fail the build.

### Response Model Generation

When `generateResponses` is enabled (default `false`), the codegen plugin generates response model data classes for each operation's selection set, annotated per the configured `serializationBackend` (strict `NONE` stays plain; `KOTLINX` emits `@Serializable`/`@SerialName`; `GSON` emits `@SerializedName`).

**Features:**
- Operation-scoped models representing the exact JSON shape of each query/mutation
- Per-backend annotations as configured; `NONE` emits plain data holders (and plain sealed interfaces for abstract types) with no serializer imports
- Alias-aware property naming (distinct property per aliased field)
- List nullability preservation (container and element nullability)
- Conditional field handling (`@include`/`@skip` → nullable types with `null` defaults)
- Sealed interface generation for GraphQL interfaces and unions; `KOTLINX` adds `@JsonClassDiscriminator("__typename")` for automatic dispatch, `NONE` emits plain sealed structures

**Usage:**
```kotlin
retrofitGraphQL {
    common {
        generateResponses.set(true)
        serializationBackend.set(SerializationBackend.KOTLINX)  // explicit; no auto-selection
    }
    target("anilist") {
        schema.set(file("src/main/graphql/anilist/schema.graphql"))
        operations.from(fileTree("src/main/graphql/anilist"))
    }
}
```

**Consumer integration (backend-neutral contracts):**
```kotlin
// No global Json configuration needed for __typename discrimination with KOTLINX:
// generated sealed interfaces carry @JsonClassDiscriminator("__typename"), so
// kotlinx.serialization automatically handles polymorphic deserialization.
//
// Declare endpoints with the neutral request/response contracts:
@POST("graphql")
suspend fun getMedia(
    @Body request: GraphQLOperationRequest<GetMediaDetailVariables>,
): GraphQLResponse<GetMediaDetailData>
```

Generated types are transport DTOs. Map them to your domain models at the Retrofit boundary rather than exposing generated classes throughout your application.

### Custom Serialization Backend

The runtime serialization layer is a backend-neutral codec SPI. If you currently use `:library` for its bundled serialization modules:

**JitPack external coordinates:**
```kotlin
// Gson codec
implementation("com.github.AniTrend.retrofit-graphql:serialization-gson:{tag}")

// kotlinx.serialization codec
implementation("com.github.AniTrend.retrofit-graphql:serialization-kotlinx:{tag}")
```

**Internal development with project references:**
```kotlin
// Gson codec (skip if unused)
implementation(project(":serialization-gson"))

// kotlinx.serialization codec
implementation(project(":serialization-kotlinx"))
```

The `:runtime` module is backend-neutral and uses no serializer by default: `GraphQLConverterFactory.create(...)` requires an explicit `GraphQLTransportCodec` instance, and the codec you pass is the only serialization path used. Custom codecs implement `GraphQLTransportCodec` (from `:serialization-api`) and own adapter resolution for parameterized types, wire names, enum handling, polymorphism, scalar mapping, and their own R8 rules. The legacy `GraphQLJson` seam (`:compat`) is separate and deprecated; it exists only for the `GraphConverter` flow.

### Minimal (Converter Only)

If you want the absolute minimum:

**JitPack external coordinates:**
```kotlin
implementation("com.github.AniTrend.retrofit-graphql:runtime:{tag}")
implementation("com.github.AniTrend.retrofit-graphql:api:{tag}")
implementation("com.github.AniTrend.retrofit-graphql:annotations:{tag}")
```

> **Note:** `:runtime` currently publishes `:android-assets` with `api` scope, so those types are already available transitively. If you want the slimmest direct dependency list, keep only `runtime` and `annotations`. Add `android-assets` explicitly only when you want that module called out in your build file.

**Internal development with project references:**
```kotlin
implementation(project(":runtime"))
implementation(project(":api"))
implementation(project(":annotations"))
```

## Backward Compatibility

The `:library` module still exists and can be used as before:

```kotlin
// JitPack (monolithic aggregator -- deprecated):
dependencies {
    implementation("com.github.AniTrend:retrofit-graphql:{tag}")
}

// Internal development:
dependencies {
    implementation(project(":library")) // Still works, but deprecated
}
```

All types under `io.github.wax911.library.*` are Kotlin type aliases pointing to their targets at `co.anitrend.retrofit.graphql.*`, now shipped from `:compat`. Using `:library` generates deprecation warnings at compile time.

> Root artifact coordinates use `com.github.AniTrend`, while module coordinates use `com.github.AniTrend.retrofit-graphql`. `:compat` publishes its own module artifact (`com.github.AniTrend.retrofit-graphql:compat`) for consumers that want the legacy surface without the aggregator.

## Consumer Code Changes

### Import Changes

Old imports (`:library` aggregator path):
```kotlin
import io.github.wax911.library.annotation.GraphQuery
import io.github.wax911.library.converter.GraphConverter
import io.github.wax911.library.model.body.GraphContainer
```

New imports (direct module paths; legacy FQCNs resolve from `:compat`):
```kotlin
import co.anitrend.retrofit.graphql.annotation.GraphQuery
import co.anitrend.retrofit.graphql.converter.GraphConverter      // legacy, :compat
import co.anitrend.retrofit.graphql.model.body.GraphContainer     // legacy, :compat
```

For the backend-neutral path:
```kotlin
import co.anitrend.retrofit.graphql.converter.GraphQLConverterFactory          // :runtime
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest      // :api
import co.anitrend.retrofit.graphql.model.GraphQLResponse                      // :api
import co.anitrend.retrofit.graphql.serialization.kotlinx.KotlinxGraphQLTransportCodec  // :serialization-kotlinx
```

### Converter Wiring

Legacy (deprecated, `:compat` -- optional registry for codegen):
```kotlin
GraphConverter(processor, json, registry = GeneratedGraphQLRegistry)
// or via static factory:
GraphConverter.create(androidContext(), registry = GeneratedGraphQLRegistry)
GraphConverter.create(registry = GeneratedGraphQLRegistry)
GraphConverter.create(gson, registry = GeneratedGraphQLRegistry)
```

Backend-neutral (new -- explicit codec required):
```kotlin
GraphQLConverterFactory.create(
    codec = KotlinxGraphQLTransportCodec(),   // or GsonGraphQLTransportCodec()
    registry = GeneratedGraphQLRegistry,
)
```

### Query Resolution (legacy path)

If you subclass the legacy `GraphRequestConverter` (`:compat`), the `resolveQuery()` method is `protected open` and checks the registry first before falling back to asset-based discovery. Override it to customize resolution. The neutral `GraphQLRequestConverter` never consults a registry because `GraphQLOperationRequest` always carries its document.

---

# Backend-Neutral Contract Migration

The serializer-coupled request/response surface (`GraphQLRequest`, `GraphContainer`, `GraphError`, `GraphQLJson`, `QueryContainerBuilder`, `GraphConverter`, and the `GsonGraphQLJson`/`KotlinxGraphQLJson` wrappers) is deprecated. New code should use the backend-neutral contracts and the explicit-codec converter:

| Legacy (deprecated, `:compat`) | Neutral (`:api` / `:runtime` / `:serialization-*`) |
|--------------------------------|----------------------------------------------------|
| `GraphQLRequest<TVariables>` | `GraphQLOperationRequest<TVariables>` |
| `GraphContainer<T>` / `GraphError` | `GraphQLResponse<T>` / `GraphQLResponseError` (+ `GraphQLData`, `GraphQLPathSegment`, `GraphQLValue`) |
| `GraphConverter` / `GraphRequestConverter` / `GraphResponseConverter` | `GraphQLConverterFactory` / `GraphQLRequestConverter` / `GraphQLResponseConverter` |
| `GraphQLJson` / `GsonGraphQLJson` / `KotlinxGraphQLJson` | `GraphQLTransportCodec` / `GsonGraphQLTransportCodec` / `KotlinxGraphQLTransportCodec` |
| `QueryContainerBuilder` / `QueryContainer` / `PersistedQuery*` | `GraphQLOperationRequest.withPersistedQuery(...)` |

## Compatibility guarantee

- Every legacy class keeps its historical fully qualified name and is shipped **exactly once**, by `:compat` only. `:api`, `:runtime`, `:serialization-api`, `:serialization-gson`, and `:serialization-kotlinx` never carry a legacy class, so the `:library` aggregate classpath has no duplicates (verified by tests in `:compat` and `:library`).
- The 37 `io.github.wax911.library.*` type aliases move with the legacy surface into `:compat` and keep their exact `@Deprecated(WARNING)` + `ReplaceWith` expressions. Type aliases produce no classes themselves; each typealias-only source file emits only an empty `*Kt` file-facade class with no members, so no keep rules are needed (the deprecated `consumer-rules.pro` rule matched only those empty facades and was removed as behaviorally inert).
- `:api`, `:serialization-api`, and `:runtime` resolve no Gson or kotlinx.serialization artifacts; `:compat` owns the legacy backend path. Both sides are enforced by classpath-scan unit tests (`BackendDependencyBoundaryTest` in the neutral modules, `CompatOwnsLegacyBackendTest` in `:compat`) that run on the resolved test runtime classpath.
- The legacy `EmptyGraphQLVariables` sentinel in `:api` is now a plain object (no `@Serializable`); the legacy `KotlinxGraphQLJson` in `:compat` special-cases it so `GraphQLRequest<EmptyGraphQLVariables>` keeps encoding as `{}` without a serializer dependency in `:api`.

## New codegen request path

Generated `.request(...)` helpers return `GraphQLOperationRequest<TVariables>`. Register the explicit codec factory on Retrofit:

```kotlin
val retrofit = Retrofit.Builder()
    .addConverterFactory(
        GraphQLConverterFactory.create(
            codec = KotlinxGraphQLTransportCodec(), // or GsonGraphQLTransportCodec()
            registry = GeneratedGraphQLRegistry,
        )
    )
    .baseUrl(baseUrl)
    .build()
```

Declare Retrofit endpoints with the neutral contracts:

```kotlin
@POST("graphql")
suspend fun getCurrentUser(
    @Body request: GraphQLOperationRequest<EmptyGraphQLVariables>,
): Response<GraphQLResponse<GetCurrentUserData>>
```

The codec path never consults the registry (the request already carries its document) and never falls back to asset discovery; the codec instance you pass is the only serialization path used.

## Legacy `GraphConverter` stays available

The deprecated `GraphConverter` (with every factory overload, Gson default, and the `GraphQLJson` seam) remains source- and binary-compatible inside `:compat`, reachable through `:library` or the `:compat` artifact. Mixing both paths in one app is supported: use `GraphQLConverterFactory` for generated endpoints and `GraphConverter` for legacy asset-based endpoints, each on its own Retrofit instance (see the sample app's `data/arch/koin/Modules.kt`).

The legacy surface (`:compat`, `:library`, the `io.github.wax911.library.*` aliases, and the `GraphConverter`/`GraphQLJson` flow) is guaranteed for the current major version; like `:library`, it will be removed in a future major release. The compatibility guarantee above keeps every legacy FQCN exactly once on the aggregate classpath until then, so migration can happen incrementally.

---

# v0.13.x Serialization Contract Migration

This section is a historical record of the v0.13.x migration from v2.x Gson-based `GraphConverter` construction to the `GraphQLJson` abstraction. **Both `GraphConverter` and `GraphQLJson` are now deprecated legacy types shipped from `:compat`**; new code should use the backend-neutral `GraphQLOperationRequest`/`GraphQLResponse`/`GraphQLTransportCodec` contracts described in the [Backend-Neutral Contract Migration](#backend-neutral-contract-migration) section above.

### Gson-to-GraphQLJson

The `GraphConverter` factory methods accept a `GraphQLJson` instance instead of a `Gson` instance. The library ships two implementations, both now living in `:compat` with the deprecated converter surface:

| Backend | Module | Class |
|---------|--------|-------|
| kotlinx.serialization | `:compat` (legacy) | `KotlinxGraphQLJson(Json)` |
| Gson | `:compat` (legacy) | `GsonGraphQLJson(Gson)` |

The optional peer modules `:serialization-kotlinx`/`:serialization-gson` no longer ship these wrappers; they ship the new transport codecs (`KotlinxGraphQLTransportCodec`/`GsonGraphQLTransportCodec`) only.

**Before (v2.x -- deprecated Gson overloads):**

```kotlin
val converter = GraphConverter.create(
    context = context,
    gson = GsonBuilder().serializeNulls().create(),
    registry = GeneratedGraphQLRegistry,
)
```

**After (v0.13.x -- new GraphQLJson overload, legacy `:compat` path):**

```kotlin
// kotlinx.serialization path
val json = KotlinxGraphQLJson(
    Json { ignoreUnknownKeys = true; encodeDefaults = false }
)
val converter = GraphConverter.create(
    context = context,
    json = json,
    registry = GeneratedGraphQLRegistry,
)

// Gson path (for request-only or upload consumers)
val json = GsonGraphQLJson(GsonBuilder().serializeNulls().create())
val converter = GraphConverter.create(
    context = context,
    json = json,
    registry = GeneratedGraphQLRegistry,
)
```

> **New code:** use the backend-neutral path instead -- `GraphQLConverterFactory.create(codec = KotlinxGraphQLTransportCodec(), registry = ...)` with `GraphQLOperationRequest`/`GraphQLResponse` contracts. The `GraphQLJson` seam above exists only for the deprecated `GraphConverter` flow in `:compat`.

**Gson overloads preserved**: The 6 existing `GraphConverter.create(context, gson, ...)` and `GraphConverter.create(gson, registry, ...)` overloads are **preserved** for backward compatibility. They internally wrap the `Gson` instance in `GsonGraphQLJson`. No migration is required if you continue using the Gson-backed overloads.

### Constructor rename (binary breaking)

**For subclassers only**: The `GraphConverter` primary constructor parameter was renamed:

- v2.x: `protected val gson: Gson`
- v0.13.x: `protected val json: GraphQLJson`

This is a **binary-compatibility** breaking change. Source-compatible code that uses `GraphConverter` factory methods (`create(...)`) is unaffected. Only direct constructor calls or subclasses that reference the `gson` parameter need updating:

```kotlin
// v2.x (broken in v0.13.x)
class CustomConverter(processor: AbstractGraphProcessor, gson: Gson) :
    GraphConverter(processor, gson)  // error: cannot find 'gson'

// v0.13.x (fix)
class CustomConverter(processor: AbstractGraphProcessor, json: GraphQLJson) :
    GraphConverter(processor, json)
```

### kotlinx parameterized type handling

`KotlinxGraphQLJson.decode()` now handles `java.lang.reflect.ParameterizedType` (e.g., `GraphContainer<GetCurrentUserData>`). Previously, kotlinx could not resolve serializers for parameterized types handed to it by Retrofit's converter infrastructure.

**How it works**: Retrofit passes the full parameterized `Type` to `GraphResponseConverter`, which forwards it to `GraphQLJson.decode(json, type)`. `KotlinxGraphQLJson` uses `kotlinx.serialization.serializer(type)` (the JVM reflection extension) to resolve the correct `KSerializer<GraphContainer<GetCurrentUserData>>`.

**Consumer impact**: None for consumers using generated response DTOs. The fix is internal. If you implemented a custom `GraphQLJson` backend, ensure your `decode(json, type)` implementation handles parameterized types.

### @Serializable on legacy API types (historical, now `:compat`)

In v0.13.x the serializer-coupled models were annotated with `@Serializable` and lived in `:api`. Since the backend-neutral split, **these types are legacy and shipped from `:compat`**; `:api` applies no kotlinx.serialization and carries no serializer dependency. Modular consumers of `:api` alone never need `kotlinx-serialization-core`.

If you still use the legacy `GraphQLJson`/`GraphConverter` path, the `:compat` module (or the `:library` aggregator) provides these types with their serializer annotations:

| Type | Annotations | Notes |
|------|------------|-------|
| `GraphContainer<T>` | `@Serializable` | `extensions` field is `@Transient` |
| `GraphQLRequest<TVariables>` | `@Serializable` | `extensions` field is `@Transient` |
| `GraphError` | `@Serializable` | `path` and `extensions` fields are `@Transient` |
| `GraphError.Location` | `@Serializable` | Nested `Location` type within `GraphError` |
| `PersistedQuery` | `@Serializable` | APQ extension payload of the `QueryContainer` flow |
| `EmptyGraphQLVariables` | none (plain object) | Previously `@Serializable`; now a plain sentinel. The legacy `KotlinxGraphQLJson` special-cases it and encodes it as `{}` |

These annotations enable the legacy kotlinx wrapper (`KotlinxGraphQLJson`) to serialize/deserialize these types when Retrofit passes `GraphContainer<GeneratedData>` as a response type. The new codec path (`GraphQLTransportCodec`) decodes the neutral `GraphQLResponse` envelope without these annotations.

### @Transient fields (legacy `:compat` models)

Several legacy fields are marked `@Transient` because their types cannot be resolved by the kotlinx.serialization compiler plugin (`Map<Any, Any>`, `List<Any>`, `Map<String, Any?>`). This applies to the deprecated serializer-coupled models in `:compat`; the neutral `GraphQLResponse`/`GraphQLValue` contract represents the same data without `@Transient` fields:

| Field | Type | Gson | kotlinx |
|-------|------|------|---------|
| `GraphContainer.extensions` | `Map<Any, Any>?` | Serialized | `@Transient` (excluded) |
| `GraphQLRequest.extensions` | `Map<String, Any?>` | Serialized | `@Transient` (excluded) |
| `GraphError.path` | `List<Any>?` | Serialized | `@Transient` (excluded) |
| `GraphError.extensions` | `Map<String, Any?>?` | Serialized | `@Transient` (excluded) |

These fields remain excluded from kotlinx deserialization. If your application depends on them being serialized or read back exactly, use the Gson-backed `GsonGraphQLJson` or the preserved Gson factory overloads (all in `:compat`). On the backend-neutral path, `GraphQLResponse.extensions`, `GraphQLResponseError.extensions`, and error `path` are represented structurally as `GraphQLValue` trees, so nothing is `@Transient`.

If you only need JSON-tree access to the transient fields on the legacy path, define an alternative transport wrapper with concrete kotlinx-compatible types instead of adding a generic custom serializer to `GraphContainer<T>` or `GraphQLRequest<TVariables>`:

```kotlin
@Serializable
data class JsonGraphContainer<T>(
    val data: T? = null,
    val errors: List<JsonGraphError>? = null,
    val extensions: JsonObject? = null,
)

@Serializable
data class JsonGraphError(
    val message: String? = null,
    val path: List<JsonElement>? = null,
    val locations: List<GraphError.Location>? = null,
    val extensions: JsonObject? = null,
)

@POST("graphql")
suspend fun getCurrentUser(
    @Body request: GraphQLRequest<EmptyGraphQLVariables>,
): Response<JsonGraphContainer<GetCurrentUserData>>
```

No custom serializer is required unless the actual wire structure needs transformation before or after normal serialization.

### APQ behavior (legacy kotlinx path)

`GraphQLRequest.withPersistedQuery()` works on the typed legacy kotlinx request path. `GraphQLRequest.extensions` remains `@Transient` on the data class, but `KotlinxGraphQLJson.encode()` merges supported extension values, including `PersistedQuery`, into the outgoing JSON.

- **Gson path**: `withPersistedQuery()` works as before -- the `extensions` map is serialized reflectively
- **kotlinx path**: `withPersistedQuery()` works for `GraphQLRequest<TVariables>` requests encoded through `KotlinxGraphQLJson`

If you depend on arbitrary extension payload shapes beyond primitives, lists, maps, `JsonElement`, or `@Serializable` values, prefer the Gson-backed path or provide a custom serializer.

On the backend-neutral path, use `GraphQLOperationRequest.withPersistedQuery(sha256Hash, version)`, which stores the extension structurally as `GraphQLValue.ObjectValue` and works with every `GraphQLTransportCodec` implementation.

### QueryContainerBuilder behavior (legacy kotlinx path)

The legacy `QueryContainerBuilder` flow stays on a Gson-backed serializer internally because `QueryContainer` is not `@Serializable` (it uses mutable properties, manual builders, and untyped maps). When using kotlinx as the serialization backend:

- Use the generated `.request(...)` factory methods (which return the neutral `GraphQLOperationRequest<TVariables>`) for the primary request path
- Existing `QueryContainerBuilder` endpoints continue to work through `:compat`, but they are encoded through Gson for backward compatibility

---

## Further Reading

- [Sample app migration](app/) — complete `:app` module migration on this branch
- [Wiki: Migration Guide](https://github.com/AniTrend/retrofit-graphql/wiki/Migration-Guide) — wiki version of this guide
- [Wiki: Home](https://github.com/AniTrend/retrofit-graphql/wiki) — usage examples and advanced topics
- [Release artifact documentation](docs/wiki/) — wiki pages for serialization backends, R8, and naming
