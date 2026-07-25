# Retrofit Converter - With GraphQL Support &nbsp; [![](https://jitpack.io/v/anitrend/retrofit-graphql.svg)](https://jitpack.io/#AniTrend/retrofit-graphql) &nbsp; [![Codacy Badge](https://app.codacy.com/project/badge/Grade/429330bfbee34bb29a74e5cd01220c68)](https://app.codacy.com/gh/AniTrend/retrofit-graphql/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade) &nbsp; [![android-unit-test](https://github.com/AniTrend/retrofit-graphql/actions/workflows/android-test.yml/badge.svg)](https://github.com/AniTrend/retrofit-graphql/actions/workflows/android-test.yml)

Seeing how we already have a really powerful type-safe HTTP client for Android and Java
[Retrofit](http://square.github.io/retrofit/) why not use it and extend it's functionality,
this project is a retrofit converter which injects `.graphql` query or mutation file contents
into a request body along with any GraphQL variables. Supports both runtime asset-based
discovery and optional build-time code generation for type-safe request helpers.

> **Note:** As of v0.13.x, retrofit-graphql offers first-class support for kotlinx.serialization with generated `@Serializable` response models, Gson-for-upload paths, and R8-safe serialization. See [MIGRATION.md](MIGRATION.md) for migration guidance from v2.x GraphQLJson + Gson workflows.

## Why This Project Exists?

Many might wonder why this exists when an android GraphQL library like [Apollo](https://github.com/apollographql/apollo-android) exists. Unfortunately Apollo for Android still lacks some basic but important features/functionality which led to the following questions about [General Design Questions Regarding Apollo](https://github.com/apollographql/apollo-android/issues/847), [Polymorphic Type Handling](https://github.com/apollographql/apollo-android/issues/334) and [Non Shared Types](https://github.com/apollographql/apollo-android/issues/898). Don't get me wrong Apollo is not inferior any way, it has amazing features such as:

- Code Generation (Classes and Data Types)
- Custom Scalar Types
- Cached Responses

But since model classes are automatically generated for you, the developer loses some flexibility, such as use of generics, abstraction and inheritance.
Also Android Performance best practice suggests that developers should use StringDef and IntDef over
enums and [here's why](https://stackoverflow.com/questions/29183904/should-i-strictly-avoid-using-enums-on-android), with the exception of kotlin
especially when using R8.

> **Note:** As of v2.x, retrofit-graphql also offers optional build-time code generation via a Gradle plugin. This combines the flexibility of file-based queries with type-safe request helpers when desired.
>
> As of v0.13.x, retrofit-graphql adds first-class kotlinx.serialization support with `@Serializable` generated response DTOs, a serialization-backend-agnostic `GraphQLJson` abstraction, and R8-safe serialization. Gson remains supported for request-only consumers and the multipart upload path. See [MIGRATION.md](MIGRATION.md) for migration guidance.

Strangely there are tons of simple examples all over Medium using apollo graphql for Android,
but none of them address these issues because most of them just construct a simple single resource
request demo application. These look just fine at first glance until you start working with multiple data types and apollo starts generating classes for every fragment and query even if the data models are the same, or share similar properties.

See a list of changes from [here](./CHANGELOG.md)
____

## How Everything Works

### Getting Started

- __Add the JitPack repository to your build file__

```javascript
allprojects {
    repositories {
        ...
        maven { url 'https://www.jitpack.io' }
    }
}
```

- __Add the dependency__

```javascript
dependencies {
    implementation 'com.github.anitrend:retrofit-graphql:{latest_version}'
}
```

### Modular Dependencies (Recommended)

As of v2.x, the library is organized into composable modules. Instead of depending on the monolithic aggregator, you can depend only on the modules you need. The JitPack coordinates follow the pattern `com.github.AniTrend.retrofit-graphql:{module}:{tag}`:

```kotlin
repositories {
    maven { url = uri("https://www.jitpack.io") }
}

dependencies {
    // Core converter (asset-based queries)
    implementation("com.github.AniTrend.retrofit-graphql:runtime:{latest_version}")

    // Public API interfaces and models
    implementation("com.github.AniTrend.retrofit-graphql:api:{latest_version}")

    // Asset-based query discovery
    implementation("com.github.AniTrend.retrofit-graphql:android-assets:{latest_version}")

    // @GraphQuery annotation (only needed for asset-based queries, not codegen)
    implementation("com.github.AniTrend.retrofit-graphql:annotations:{latest_version}")

    // Optional: serialization backends
    // implementation("com.github.AniTrend.retrofit-graphql:serialization-gson:{latest_version}")
    // implementation("com.github.AniTrend.retrofit-graphql:serialization-kotlinx:{latest_version}")
}
```

> **Note:** `:runtime` currently publishes `:android-assets` with `api` scope, so those types are already available transitively to consumers. Codegen-only consumers do not need direct `:android-assets` or `:annotations` dependencies — they are pulled transitively via `:runtime`. Keep direct dependencies only when you use the asset-based `@GraphQuery` workflow alongside codegen.

> **For internal development in this repository**, use project references instead of JitPack coordinates:
> ```kotlin
> dependencies {
>     implementation(project(":runtime"))
>     implementation(project(":api"))
>     implementation(project(":android-assets"))
>     implementation(project(":annotations"))
> }
> ```

The deprecated monolithic aggregator remains available for backward compatibility:

```kotlin
dependencies {
    implementation("com.github.AniTrend:retrofit-graphql:{latest_version}")
}
```

> Root artifact coordinates use `com.github.AniTrend`, while module coordinates use `com.github.AniTrend.retrofit-graphql`.

For code generation support, apply the Gradle plugin and add a `retrofitGraphQL { }` config block. The plugin generates operation constants, a document registry, enum classes, variable classes, typed request helpers, and optionally response model data classes. See [MIGRATION.md](MIGRATION.md) for the full migration guide and the [wiki Code Generation page](https://github.com/AniTrend/retrofit-graphql/wiki/Codegen) for the DSL reference.

### Serialization Backend Configuration

The codegen plugin emits serialization annotations (`@Serializable`/`@SerialName` for kotlinx, `@SerializedName` for Gson) on generated types. The `serializationBackend` property selects which backend to use:

```kotlin
retrofitGraphQL {
    common {
        serializationBackend.set(SerializationBackend.KOTLINX)  // or .GSON or .NONE
    }
}
```

| Value | Behavior |
|-------|----------|
| `NONE` | No serialization annotations emitted. Classes are generated as plain data holders. |
| `KOTLINX` | Emits `@Serializable`, `@SerialName`, and polymorphic markers. Required for response models that include interface or union paths. |
| `GSON` | Emits `@SerializedName` on properties. Supports response models for concrete-only operations (no interface/union types). |

**Auto-selection (convenience)**: When `serializationBackend` is `NONE` (the default) and `generateResponses` is `true`, the codegen automatically selects `KOTLINX`. This means consumers who only care about typed responses can set `generateResponses.set(true)` without explicitly configuring the backend. Consumers who want Gson annotations for variables/input objects without response models should set `serializationBackend.set(SerializationBackend.GSON)` and leave `generateResponses` at `false`.

The `serializationBackend` property can be set in `common {}` (applies to all targets) and overridden per-target in `target("name") { }` blocks.

### Response Model Generation (Opt-in)

When `generateResponses` is enabled, the codegen plugin generates kotlinx-serializable response model data classes from the operation selection sets. Each operation produces a `{OperationName}Data` root class with nested data classes for every selected GraphQL object type.

```kotlin
retrofitGraphQL {
    common {
        generateResponses.set(true)  // default false
        // serializationBackend automatically selects KOTLINX when generateResponses=true
    }
    target("anilist") {
        schema.set(file("src/main/graphql/schema.graphql"))
        operations.from(fileTree("src/main/graphql"))
    }
}
```

Generated response models:
- Use `@Serializable` and `@SerialName` annotations for kotlinx serialization
- Preserve GraphQL aliases as distinct Kotlin properties
- Handle list nullability (container + element)
- Support conditional fields (`@include`/`@skip`) with nullable types
- Generate sealed interfaces for GraphQL interfaces and unions with `__typename`-based polymorphism
- Escape Kotlin keywords by appending `Value` suffix (e.g. `private` -> `privateValue`) while preserving the original wire name in `@SerialName`

**Using generated response DTOs with Retrofit**:

```kotlin
// Retrofit interface: declare GraphContainer<GeneratedOperationData> as the return type
internal interface UserRemoteSource {
    @POST("graphql")
    suspend fun getCurrentUser(
        @Body request: GraphQLRequest<EmptyGraphQLVariables>
    ): Response<GraphContainer<GetCurrentUserData>>
}

// Generated types are kotlinx.serialization-compatible transport DTOs
// Map them to your domain models at the Retrofit boundary:
class UserResponseMapper : GraphQLMapper<GetCurrentUserData, UserEntity>() {
    override suspend fun onResponseMapFrom(source: GetCurrentUserData): UserEntity {
        val viewer = source.viewer ?: throw IllegalStateException("No viewer data")
        return UserEntity(
            id = viewer.id,
            login = viewer.login,
            name = viewer.name,
            bio = viewer.bio,
        )
    }
}
```

Generated types are transport DTOs designed for the Retrofit/network boundary. Map them to your domain models rather than exposing generated classes throughout your application.

### Serialization: kotlinx vs Gson

**Recommended production path**: Use `kotlinx.serialization` (`:serialization-kotlinx`) for the Retrofit response path. It offers:

- Compile-time serializer generation -- no reflection, R8-safe by default
- Automatic `@SerialName` matching on generated response DTOs
- No need for custom keep rules on generated types

**Gson compatibility**: Gson (`:serialization-gson`) remains supported for:

- **Request-only consumers**: When you only serialize `GraphQLRequest<T>` bodies (no response deserialization needed), Gson works the same as before
- **Multipart upload path**: The `UploadMutationHelper` sample uses Gson for serializing the operations payload in multipart requests. Two targeted R8 keep rules are needed for this path (see R8 section below)
- **Legacy `QueryContainerBuilder` flow**: The `QueryContainer` class is not `@Serializable`, so the runtime keeps this builder-based flow on an internal Gson serializer even when responses use `KotlinxGraphQLJson`

**Gson + generated responses**: Supported for concrete-only operations and covered by codegen functional tests. Operations that include GraphQL interfaces or unions fail during codegen with a diagnostic that includes the operation name and response path. Use `KOTLINX` for those polymorphic response DTOs.

See the [Serialization Backends](docs/wiki/Serialization-Backends.md) wiki page for detailed compatibility matrix.

More wiki documentation:
- [Code Generation](docs/wiki/Codegen.md) -- plugin setup, DSL, and generated output
- [Generated Response DTOs](docs/wiki/Generated-Response-DTOs.md) -- using generated response models with Retrofit
- [Naming Contract](docs/wiki/Naming-Contract.md) -- Kotlin name, wire name, and descriptor name rules
- [R8 / ProGuard](docs/wiki/R8-ProGuard.md) -- R8 configuration and keep rules
- [Parameterized Types](docs/wiki/Parameterized-Types.md) -- how parameterized types flow through serialization

### R8 / ProGuard

R8 is the recommended code shrinker for Android release builds. The sample app demonstrates a working R8 configuration:

```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}
```

**kotlinx.serialization: No custom rules needed**. The kotlinx.serialization compiler plugin generates R8-consumer-rules that automatically keep `@Serializable` classes and their serial descriptors. Generated response DTOs, `GraphContainer`, `GraphError`, `GraphQLRequest`, and `EmptyGraphQLVariables` are all protected by these consumer rules.

**Gson upload path: 2 targeted keep rules needed**. The sample app uses Gson for the multipart upload path (`UploadMutationHelper`). Because Gson uses reflection (`java.lang.reflect.Field.getName()`) to derive JSON keys, field names must survive R8 renaming. Two keep rules are required:

```proguard
# GraphQLRequest: serialized by Gson in UploadMutationHelper.createOperationsPart()
-keep class co.anitrend.retrofit.graphql.model.GraphQLRequest {
    <fields>;
    <init>(...);
}

# UploadToStorageBucketVariables: serialized by Gson as nested variables
-keep class co.anitrend.retrofit.graphql.sample.bucket.UploadToStorageBucketVariables {
    <fields>;
    <init>(...);
}
```

These rules are narrowly scoped to the exact classes used in the Gson upload path. If your project uses Gson for the entire Retrofit path (no kotlinx), you may need broader keep rules for your model classes. The library's own consumer-rules are shipped from `:library` and are included automatically.

**APQ + Gson trap**: `GraphQLRequest.withPersistedQuery()` works on the typed kotlinx request path because `KotlinxGraphQLJson.encode()` merges supported `extensions` entries into the outgoing JSON. If you use APQ in the Gson upload path, add:

```proguard
-keep class co.anitrend.retrofit.graphql.model.request.PersistedQuery { <fields>; }
```

**Verifying R8 safety**: The sample app separates release-variant JVM checks from the R8 runtime gate:

- `./gradlew :app:testReleaseUnitTest` -- runs release-variant JVM unit tests (11 tests: 6 serialization + 5 mapper). These do not execute minified R8 bytecode.
- `./gradlew :app:releaseR8Verification` -- runs the sample app's R8 runtime gate: mapping checks plus the managed-device `pixel2api30ReleaseAndroidTest` task against the R8-optimized APK.
- Inspect `app/build/outputs/mapping/release/mapping.txt` to verify no serialization-critical fields are renamed

The release verification covers the kotlinx generated response runtime plus the sample's Gson multipart upload path. Concrete Gson response DTO generation is covered by codegen functional tests; consumers using Gson response DTOs in release should add their own scoped keep-rule verification.

### Gradle Plugin Consumption

The codegen plugin can now be consumed through the Gradle `plugins {}` DSL.

```kotlin
pluginManagement {
    repositories {
        mavenLocal() // optional for local verification / development
        maven(url = uri("https://jitpack.io"))
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    id("co.anitrend.retrofit.graphql.codegen") version "{latest_version}"
}
```

> **JitPack note:** this repository now ships a root `.jitpack.yml` that runs both the main build publication and the standalone `gradle-plugin` publication path. Local verification in this repository resolves the plugin from `mavenLocal()` using the published plugin marker artifact; remote JitPack resolution should follow the same published metadata flow.

### Converter Factories for Codegen Registries

Codegen-only consumers no longer need an Android `Context` just to use the generated registry:

```kotlin
val converter = GraphConverter.create(registry = GeneratedGraphQLRegistry)

val customGsonConverter = GraphConverter.create(
    gson = GsonBuilder().serializeNulls().create(),
    registry = GeneratedGraphQLRegistry,
)
```

If you want generated documents first **and** asset-based fallback for mixed migrations, keep using the context-backed overload:

```kotlin
val converter = GraphConverter.create(
    context = context,
    registry = GeneratedGraphQLRegistry,
)
```

In registry-only mode, if a `@GraphQuery`-annotated operation is missing from the registry, the converter throws `IllegalStateException`. Non-annotated operations (codegen-only consumers using `GraphQLRequest<T>`) proceed without a query lookup. See [MIGRATION.md](MIGRATION.md) for details.

- __Optional R8 / ProGuard Rules__

If you are using R8 the shrinking and obfuscation rules are included automatically.

ProGuard users must manually copy the options from [this file](https://github.com/anitrend/retrofit-graphql/blob/master/library/proguard-rules.pro).

> Currently shipped from the `:library` module. This path may change in a future release.

> You might also need [retrofit rules](https://github.com/square/retrofit/blob/master/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro) and it's dependencies (OkHttp and Okio)

### Examples

Define, your `.graphql` files for your GraphQL queries, fragments, and mutations e.t.c.

If you use insomnia you can use this tool to generate your workspaces into directories and files
[insomnia-graphql-generator](https://github.com/anitrend/insomnia-graphql-generator). After you can
simply place the generated content into your assets folder e.g.:

<img src="./images/screenshots/assets_files.png" width=250 />

> **N.B.** You might find this too useful too JetBrains [JS GraphQL - Plugin](https://plugins.jetbrains.com/plugin/8097-js-graphql)

For more instructions on how to setup the sample app and other examples with code generation,
file uploads, persisted queries, custom loggers, and custom graphql files location please visit
the [projects wiki page](https://github.com/AniTrend/retrofit-graphql/wiki)

#### Screenshots

<img src="./images/screenshots/sample_img_001.png" width=250 /> <img src="./images/screenshots/sample_img_002.png" width=250 />
<img src="./images/screenshots/sample_img_003.png" width=250 /> <img src="./images/screenshots/sample_img_004.png" width=250 />
