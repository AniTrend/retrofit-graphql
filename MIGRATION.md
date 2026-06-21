# Migration Guide: From `:library` to Modular Dependencies

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
plugins {
    id("co.anitrend.retrofit.graphql.codegen") version "{tag}"
}

dependencies {
    implementation("com.github.AniTrend.retrofit-graphql:runtime:{tag}")
    implementation("com.github.AniTrend.retrofit-graphql:api:{tag}")
    implementation("com.github.AniTrend.retrofit-graphql:annotations:{tag}")
}

retrofitGraphQL {
    common {
        generateVariables.set(false)
    }
    packageName.set("your.package.generated")
    schema.set(file("src/main/graphql/schema.graphql"))
    operations.from(fileTree("src/main/graphql") {
        include("**/*.graphql")
    })
}
```

> **Note:** Code generation removes the need to declare `android-assets` directly. `:runtime` still brings it in transitively today because `GraphConverter` publicly accepts `AbstractGraphProcessor`, but that is a module-boundary detail, not a signal that codegen still relies on asset discovery.

Place your `.graphql` files in `src/main/graphql/` instead of `assets/graphql/`.

Generated output (under `build/generated/source/graphql/`) includes:
- `GeneratedGraphQLRegistry` — build-time operation registry
- `GraphQLOperations`, `GraphQLDocuments`, `GraphQLHashes` — operation constants
- Variable classes and typed request helpers (when `generateVariables = true`)

Wire the registry into your converter via Koin or manual construction:
```kotlin
// Koin
single<GraphQLDocumentRegistry> { GeneratedGraphQLRegistry }
factory { SampleConverterFactory(processor = get(), registry = get()) }

// Manual
val factory = SampleConverterFactory(
    processor = GraphProcessor(AssetManagerDiscoveryPlugin(context.assets)),
    registry = GeneratedGraphQLRegistry,
)
```

See the [sample app](app/) for a complete migration example.

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
```

### Query Resolution

If you subclass `GraphRequestConverter`, the `resolveQuery()` method is now `protected open` and checks the registry first before falling back to asset-based discovery. Override it to customize resolution.

## Further Reading

- [Sample app migration](app/) — complete `:app` module migration on this branch
- [Wiki: Migration Guide](https://github.com/AniTrend/retrofit-graphql/wiki/Migration-Guide) — wiki version of this guide
- [Wiki: Home](https://github.com/AniTrend/retrofit-graphql/wiki) — usage examples and advanced topics
