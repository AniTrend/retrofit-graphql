# Migration Guide

This guide covers migrating consumer projects from the deprecated `:library` aggregator to the new modular architecture, and from v2.x Gson-based workflows to v3.x GraphQLJson abstraction with kotlinx.serialization support.

## Table of Contents

- [Migration to Modular Dependencies](#migration-to-modular-dependencies) (v2.x)
- [v3.x Serialization Contract Migration](#v3x-serialization-contract-migration)
  - [Gson-to-GraphQLJson](#gson-to-graphqljson)
  - [Constructor rename (binary breaking)](#constructor-rename-binary-breaking)
  - [kotlinx parameterized type handling](#kotlinx-parameterized-type-handling)
  - [@Serializable on API types](#serializable-on-api-types)
  - [@Transient fields](#transient-fields)
  - [APQ behavior (kotlinx)](#apq-behavior-kotlinx)
  - [QueryContainerBuilder limitation (kotlinx)](#querycontainerbuilder-limitation-kotlinx)

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
| `:api` | Android | Public API interfaces and models (`GraphQLOperation`, `GraphQLDocumentRegistry`, `QueryContainerBuilder`) | (none) |
| `:android-assets` | Android | Runtime asset-based query discovery (`GraphProcessor`, `AssetManagerDiscoveryPlugin`, APQ) | `:annotations` |
| `:runtime` | Android | Retrofit converter (`GraphConverter`, `GraphRequestConverter`) | `:api`, `:android-assets`, `:annotations` |
| `:codegen-core` | JVM | Build-time code generation engine (parses `.graphql`, generates Kotlin) | (external: graphql-java, KotlinPoet) |
| `:gradle-plugin` | Gradle | Plugin (`id("co.anitrend.retrofit.graphql.codegen")`) that runs codegen as a Gradle task | `:codegen-core` |
| `:serialization-gson` | Android | Gson-backed `GraphQLJson` implementation | `:api` |
| `:serialization-kotlinx` | Android | kotlinx.serialization-backed `GraphQLJson` implementation | `:api` |
| `:library` | Android | **Deprecated** — backward-compatible aggregator (transitively re-exports all modules via `api()`) | All of the above |

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
}
```

> **Note:** `:runtime` currently publishes `:android-assets` with `api` scope, so those types are already available transitively. Keep a direct `android-assets` dependency only when you want that module called out explicitly in your build. Even with build-time code generation, `android-assets` is still pulled transitively today because `GraphConverter` exposes `AbstractGraphProcessor` in its public API.

**After (internal development with project references):**
```kotlin
dependencies {
    implementation(project(":runtime"))       // GraphConverter, GraphRequestConverter
    implementation(project(":api"))            // QueryContainerBuilder, GraphQLDocumentRegistry
    implementation(project(":android-assets")) // GraphProcessor, AssetManagerDiscoveryPlugin
    implementation(project(":annotations"))    // @GraphQuery
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

> **Note:** Code generation removes the need to declare `android-assets` directly. `:runtime` still brings it in transitively today because `GraphConverter` publicly accepts `AbstractGraphProcessor`, but that is a module-boundary detail, not a signal that codegen still relies on asset discovery.
> **Note:** The `:annotations` dependency (for `@GraphQuery`) is also not required for codegen-only consumers. It remains useful for projects that use a mix of asset-based and codegen workflows.

Place your `.graphql` files in `src/main/graphql/` instead of `assets/graphql/`.

Generated output (under `build/generated/source/graphql/`) includes:
- `GeneratedGraphQLRegistry` — build-time operation registry implementing `GraphQLDocumentRegistry`
- `GraphQLOperations`, `GraphQLDocuments`, `GraphQLHashes` — operation constants
- Enum classes — GraphQL enum types generated as Kotlin enum classes
- Variable classes — typed data classes implementing `GraphQLVariables` (when `generateVariables = true`)
- Input object classes — typed data classes for GraphQL input types (when `generateVariables = true`)
- Request helpers — `.request(...)` factory methods on operation objects returning `GraphQLRequest<VariableType>` (when `generateVariables = true`)
- Response model classes — kotlinx-serializable `{OperationName}Data` classes with nested types for selected fields (when `generateResponses = true`)

Wire the registry into your converter via Koin or manual construction:
```kotlin
// Koin, registry-only (no Context required)
single<GraphQLDocumentRegistry> { GeneratedGraphQLRegistry }
factory { GraphConverter.create(registry = get()) }

// Koin, mixed asset + codegen fallback
factory { GraphConverter.create(context = get(), registry = get()) }

// Manual, registry-only
val registryOnlyFactory = GraphConverter.create(
    registry = GeneratedGraphQLRegistry,
)

// Manual, custom Gson + registry-only
val customGsonFactory = GraphConverter.create(
    gson = GsonBuilder().serializeNulls().create(),
    registry = GeneratedGraphQLRegistry,
)

// Manual, mixed asset + codegen fallback
val mixedFactory = GraphConverter.create(
    context = context,
    registry = GeneratedGraphQLRegistry,
)
```

> **Note:** `GraphConverter.create(registry)` and `GraphConverter.create(gson, registry)` are the new codegen-first factories. Use `GraphConverter.create(context, registry)` when you still want asset-based fallback during a mixed migration.
> **Note:** In registry-only mode, if a `@GraphQuery`-annotated operation is missing from the registry, the converter throws `IllegalStateException`. Non-annotated operations (codegen-only consumers using `GraphQLRequest<T>`) proceed without a query lookup because the request already contains the document string.

### Multipart Uploads

For file uploads, register a `RequestBodyPassThroughConverterFactory` before the `GraphConverter` so that `MultipartBody` and `RequestBody` instances bypass GraphQL conversion:

```kotlin
val retrofit = Retrofit.Builder()
    .addConverterFactory(RequestBodyPassThroughConverterFactory())
    .addConverterFactory(GraphConverter.create(context, registry = GeneratedGraphQLRegistry))
    .baseUrl(baseUrl)
    .build()
```

Use generated operation metadata for the upload mutation:
```kotlin
val request = UploadToStorageBucket.request(upload = filePath)
// Build MultipartBody from the GraphQLRequest fields
```

See the [sample app](app/) for a complete migration example.

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

When `generateResponses` is enabled (default `false`), the codegen plugin generates kotlinx-serializable response model data classes for each operation's selection set.

**Features:**
- Operation-scoped models representing the exact JSON shape of each query/mutation
- `@Serializable` and `@SerialName` annotations for kotlinx serialization
- Alias-aware property naming (distinct property per aliased field)
- List nullability preservation (container and element nullability)
- Conditional field handling (`@include`/`@skip` → nullable types with `null` defaults)
- Sealed interface generation for GraphQL interfaces and unions with `__typename`-based polymorphism

**Usage:**
```kotlin
retrofitGraphQL {
    common {
        generateResponses.set(true)
    }
    target("anilist") {
        schema.set(file("src/main/graphql/anilist/schema.graphql"))
        operations.from(fileTree("src/main/graphql/anilist"))
    }
}
```

**Consumer integration:**
```kotlin
// No global Json configuration needed for __typename discrimination.
// Generated sealed interfaces carry @JsonClassDiscriminator("__typename"),
// so kotlinx.serialization automatically handles polymorphic deserialization
// without any manual Json configuration.
//
// Decode responses into generated types as normal:
@GET("graphql")
suspend fun getMedia(@Body request: GraphQLRequest<GetMediaDetailVariables>): GraphContainer<GetMediaDetailData>
```

Generated types are transport DTOs. Map them to your domain models at the Retrofit boundary rather than exposing generated classes throughout your application.

### Custom Serialization Backend

If you currently use `:library` for its bundled serialization modules:

**JitPack external coordinates:**
```kotlin
// Gson users
implementation("com.github.AniTrend.retrofit-graphql:serialization-gson:{tag}")

// kotlinx.serialization users
implementation("com.github.AniTrend.retrofit-graphql:serialization-kotlinx:{tag}")
```

**Internal development with project references:**
```kotlin
// Gson users (already provided by Retrofit's own Gson converter -- skip if unused)
implementation(project(":serialization-gson"))

// kotlinx.serialization users
implementation(project(":serialization-kotlinx"))
```

Most consumers do **not** need these. The `:runtime` module uses Gson internally by default.

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

All types under `io.github.wax911.library.*` are now type aliases pointing to their new locations at `co.anitrend.retrofit.graphql.*`. Using `:library` generates deprecation warnings at compile time.

> Root artifact coordinates use `com.github.AniTrend`, while module coordinates use `com.github.AniTrend.retrofit-graphql`.

## Consumer Code Changes

### Import Changes

Old imports (`:library` aggregator path):
```kotlin
import io.github.wax911.library.annotation.GraphQuery
import io.github.wax911.library.converter.GraphConverter
import io.github.wax911.library.model.body.GraphContainer
```

New imports (direct module paths):
```kotlin
import co.anitrend.retrofit.graphql.annotation.GraphQuery
import co.anitrend.retrofit.graphql.converter.GraphConverter
import co.anitrend.retrofit.graphql.model.body.GraphContainer
```

### Converter Wiring

Old (no registry):
```kotlin
GraphConverter(processor, gson)
```

New (with optional registry for codegen):
```kotlin
GraphConverter(processor, gson, registry = GeneratedGraphQLRegistry)
// or via static factory:
GraphConverter.create(androidContext(), registry = GeneratedGraphQLRegistry)
GraphConverter.create(registry = GeneratedGraphQLRegistry)
GraphConverter.create(gson, registry = GeneratedGraphQLRegistry)
```

### Query Resolution

If you subclass `GraphRequestConverter`, the `resolveQuery()` method is now `protected open` and checks the registry first before falling back to asset-based discovery. Override it to customize resolution.

---

# v3.x Serialization Contract Migration

This section covers migrating from v2.x Gson-based `GraphConverter` construction to the v3.x `GraphQLJson` abstraction, and related serialization contract changes.

### Gson-to-GraphQLJson

The `GraphConverter` factory methods now accept a `GraphQLJson` instance instead of a `Gson` instance. The library ships two implementations:

| Backend | Module | Class |
|---------|--------|-------|
| kotlinx.serialization | `:serialization-kotlinx` | `KotlinxGraphQLJson(Json)` |
| Gson | `:serialization-gson` | `GsonGraphQLJson(Gson)` |

**Before (v2.x -- deprecated Gson overloads):**

```kotlin
val converter = GraphConverter.create(
    context = context,
    gson = GsonBuilder().serializeNulls().create(),
    registry = GeneratedGraphQLRegistry,
)
```

**After (v3.x -- new GraphQLJson overload):**

```kotlin
// kotlinx.serialization path (recommended for response DTOs)
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

**Gson overloads preserved**: The 6 existing `GraphConverter.create(context, gson, ...)` and `GraphConverter.create(gson, registry, ...)` overloads are **preserved** for backward compatibility. They internally wrap the `Gson` instance in `GsonGraphQLJson`. No migration is required if you continue using the Gson-backed overloads.

### Constructor rename (binary breaking)

**For subclassers only**: The `GraphConverter` primary constructor parameter was renamed:

- v2.x: `protected val gson: Gson`
- v3.x: `protected val json: GraphQLJson`

This is a **binary-compatibility** breaking change. Source-compatible code that uses `GraphConverter` factory methods (`create(...)`) is unaffected. Only direct constructor calls or subclasses that reference the `gson` parameter need updating:

```kotlin
// v2.x (broken in v3.x)
class CustomConverter(processor: AbstractGraphProcessor, gson: Gson) :
    GraphConverter(processor, gson)  // error: cannot find 'gson'

// v3.x (fix)
class CustomConverter(processor: AbstractGraphProcessor, json: GraphQLJson) :
    GraphConverter(processor, json)
```

### kotlinx parameterized type handling

`KotlinxGraphQLJson.decode()` now handles `java.lang.reflect.ParameterizedType` (e.g., `GraphContainer<GetCurrentUserData>`). Previously, kotlinx could not resolve serializers for parameterized types handed to it by Retrofit's converter infrastructure.

**How it works**: Retrofit passes the full parameterized `Type` to `GraphResponseConverter`, which forwards it to `GraphQLJson.decode(json, type)`. `KotlinxGraphQLJson` uses `kotlinx.serialization.serializer(type)` (the JVM reflection extension) to resolve the correct `KSerializer<GraphContainer<GetCurrentUserData>>`.

**Consumer impact**: None for consumers using generated response DTOs. The fix is internal. If you implemented a custom `GraphQLJson` backend, ensure your `decode(json, type)` implementation handles parameterized types.

### @Serializable on API types

The following API types are now annotated with `@Serializable` for kotlinx.serialization compatibility:

| Type | Annotations | Notes |
|------|------------|-------|
| `GraphContainer<T>` | `@Serializable` | `extensions` field is `@Transient` |
| `GraphQLRequest<TVariables>` | `@Serializable` | `extensions` field is `@Transient` |
| `GraphError` | `@Serializable` | `path` and `extensions` fields are `@Transient` |
| `GraphError.Location` | `@Serializable` | Nested `Location` type within `GraphError` |
| `EmptyGraphQLVariables` | `@Serializable` | Object type, no fields |

These annotations enable kotlinx.serialization to serialize/deserialize these types directly, which is needed when Retrofit passes `GraphContainer<GeneratedData>` as a response type.

### @Transient fields

Several fields are marked `@Transient` because their types cannot be resolved by the kotlinx.serialization compiler plugin (`Map<Any, Any>`, `List<Any>`, `Map<String, Any?>`):

| Field | Type | Gson | kotlinx |
|-------|------|------|---------|
| `GraphContainer.extensions` | `Map<Any, Any>?` | Serialized | `@Transient` (excluded) |
| `GraphQLRequest.extensions` | `Map<String, Any?>` | Serialized | `@Transient` (excluded) |
| `GraphError.path` | `List<Any>?` | Serialized | `@Transient` (excluded) |
| `GraphError.extensions` | `Map<String, Any?>?` | Serialized | `@Transient` (excluded) |

These fields remain excluded from kotlinx deserialization. If your application depends on them being serialized or read back exactly, use the Gson-backed `GsonGraphQLJson` or the preserved Gson factory overloads.

### APQ behavior (kotlinx)

`GraphQLRequest.withPersistedQuery()` still works on the typed kotlinx request path. `GraphQLRequest.extensions` remains `@Transient` on the data class, but `KotlinxGraphQLJson.encode()` merges supported extension values, including `PersistedQuery`, into the outgoing JSON.

- **Gson path**: `withPersistedQuery()` works as before -- the `extensions` map is serialized reflectively
- **kotlinx path**: `withPersistedQuery()` works for `GraphQLRequest<TVariables>` requests encoded through `KotlinxGraphQLJson`

If you depend on arbitrary extension payload shapes beyond primitives, lists, maps, `JsonElement`, or `@Serializable` values, prefer the Gson-backed path or provide a custom serializer.

### QueryContainerBuilder behavior (kotlinx)

The legacy `QueryContainerBuilder` flow stays on a Gson-backed serializer internally because `QueryContainer` is not `@Serializable` (it uses mutable properties, manual builders, and untyped maps). When using kotlinx as the serialization backend:

- Use `GraphQLRequest<TVariables>` and the generated `.request(...)` factory methods for the primary request path
- Existing `QueryContainerBuilder` endpoints continue to work, but they are encoded through Gson for backward compatibility

---

## Further Reading

- [Sample app migration](app/) — complete `:app` module migration on this branch
- [Wiki: Migration Guide](https://github.com/AniTrend/retrofit-graphql/wiki/Migration-Guide) — wiki version of this guide
- [Wiki: Home](https://github.com/AniTrend/retrofit-graphql/wiki) — usage examples and advanced topics
- [Release artifact documentation](docs/wiki/) — wiki pages for serialization backends, R8, and naming
