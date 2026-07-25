# Serialization Backends

retrofit-graphql uses a pluggable `GraphQLJson` abstraction for request and response body serialization. Two implementations are provided, and consumers can implement custom backends.

## Built-in Backends

| Backend | Module | Annotation on Generated Types | Response Models | R8 Safety |
|---------|--------|------------------------------|----------------|-----------|
| kotlinx.serialization | `:serialization-kotlinx` | `@Serializable`, `@SerialName` | Supported | Automatic |
| Gson | `:serialization-gson` | `@SerializedName` | Concrete operations only | Keep rules may be needed |

## kotlinx.serialization (Recommended)

### Setup

```kotlin
dependencies {
    implementation("com.github.AniTrend.retrofit-graphql:runtime:{tag}")
    implementation("com.github.AniTrend.retrofit-graphql:serialization-kotlinx:{tag}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
```

The `:api` module's public models are kotlinx-enabled in v0.13.x. Keep `kotlinx-serialization-core` on the runtime classpath even when you depend on `:api` directly or use the Gson backend, because those public model classes reference kotlinx serialization annotations and runtime types.

In your Gradle module, apply the kotlinx.serialization plugin:

```kotlin
plugins {
    kotlin("plugin.serialization") version "{kotlin_version}"
}
```

### Converter Wiring

```kotlin
val json = KotlinxGraphQLJson(
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
)
val converter = GraphConverter.create(
    context = context,
    json = json,
    registry = GeneratedGraphQLRegistry,
)
```

### Generated Annotations

When `serializationBackend` is `KOTLINX` (or auto-selected), generated types carry:

```kotlin
@Serializable
data class GetCurrentUserData(
    @SerialName("viewer")
    val viewer: Viewer?,
)
```

### What Works

- All generated response DTOs with `@Serializable`
- `GraphContainer<T>` responses where `T` is `@Serializable`
- `GraphQLRequest<TVariables>` request serialization
- `GraphError` and `GraphError.Location` deserialization
- `EmptyGraphQLVariables` (serializes as `{}`)
- Parameterized types via `kotlinx.serialization.serializer(type)` (JVM reflection extension)
- Polymorphic sealed interfaces with `@JsonClassDiscriminator("__typename")`

### Limitations

| Field | Reason |
|-------|--------|
| `GraphContainer.extensions` (`Map<Any, Any>`) | Cannot be statically resolved by compiler plugin |
| `GraphQLRequest.extensions` (`Map<String, Any?>`) | Cannot be statically resolved by compiler plugin |
| `GraphError.path` (`List<Any>`) | Cannot be statically resolved by compiler plugin |
| `GraphError.extensions` (`Map<String, Any?>`) | Cannot be statically resolved by compiler plugin |

These fields are marked `@Transient` and are excluded from kotlinx deserialization. Use the Gson backend if you need them preserved exactly on both encode and decode paths.

### APQ Behavior

`GraphQLRequest.withPersistedQuery()` works on the typed kotlinx request path. `GraphQLRequest.extensions` remains `@Transient` on the data class, but `KotlinxGraphQLJson.encode()` merges supported extension values, including `PersistedQuery`, into the outgoing JSON.

### R8

No custom keep rules are needed. The kotlinx.serialization compiler plugin generates consumer rules that automatically protect `@Serializable` classes and their serial descriptors.

## Gson

### Setup

```kotlin
dependencies {
    implementation("com.github.AniTrend.retrofit-graphql:runtime:{tag}")
    implementation("com.github.AniTrend.retrofit-graphql:serialization-gson:{tag}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
}
```

### Converter Wiring

```kotlin
val json = GsonGraphQLJson(
    GsonBuilder()
        .enableComplexMapKeySerialization()
        .serializeNulls()
        .create()
)
val converter = GraphConverter.create(
    context = context,
    json = json,
    registry = GeneratedGraphQLRegistry,
)
```

### Generated Annotations

When `serializationBackend` is `GSON`, generated types carry `@SerializedName`:

```kotlin
data class GetCurrentUserData(
    @SerializedName("viewer")
    val viewer: Viewer?,
)
```

### What Works

- All fields on all API types, including `extensions`, `path`
- Generated response DTOs for operations whose response selection contains only concrete object types
- `QueryContainerBuilder` (legacy builder flow stays on Gson internally)
- Multipart upload payloads

### Limitations

| Limitation | Detail |
|-----------|--------|
| Interface or union response paths | Not supported. Gson cannot deserialize the generated polymorphic sealed interfaces used for GraphQL interfaces and unions. |
| R8 | Reflection-based; may need keep rules for serialized classes. |

When `serializationBackend = GSON` and `generateResponses = true`, the codegen task validates each operation before writing output. Concrete-only operations succeed and emit `@SerializedName` response DTOs. Operations with interface or union response paths fail at build time with a diagnostic that includes the operation name and exact response path, for example `Operation 'Search' has abstract type(s) at response path(s): search (SearchResult)`.

Concrete Gson response DTO generation is covered by codegen functional tests. The sample release verification focuses on the kotlinx generated response runtime and the Gson multipart upload path because the sample app does not deserialize generated response DTOs through Gson.

### R8

The sample app requires 2 targeted keep rules for the Gson upload path:

```proguard
-keep class co.anitrend.retrofit.graphql.model.GraphQLRequest {
    <fields>;
    <init>(...);
}
-keep class co.anitrend.retrofit.graphql.sample.bucket.UploadToStorageBucketVariables {
    <fields>;
    <init>(...);
}
```

The sample release verification covers kotlinx generated response DTOs and the Gson multipart upload path. Concrete Gson response DTO support is covered by codegen functional tests, not by the sample release gate. If your project uses Gson generated response DTOs in release, keep rules should be scoped to the generated DTOs you deserialize reflectively and verified by your own release test or mapping check.

## Custom Backends

Implement `GraphQLJson` for any JSON framework:

```kotlin
class MoshiGraphQLJson(private val moshi: Moshi) : GraphQLJson {
    override fun <T : Any> encode(value: T, type: Type?): String {
        val resolvedType = type ?: value::class.java
        val adapter = moshi.adapter<T>(resolvedType)
        return adapter.toJson(value)
    }
    override fun <T : Any> decode(json: String, type: Type): T {
        val adapter = moshi.adapter<T>(type)
        return adapter.fromJson(json) ?: throw IllegalStateException("Null response")
    }
}
```

Register:

```kotlin
GraphConverter.create(context, json = MoshiGraphQLJson(moshi), registry = ...)
```

**Important**: Your `decode(json, type)` implementation must handle `java.lang.reflect.ParameterizedType` when `type` is a generic like `GraphContainer<Foo>`. The type is provided by Retrofit's converter infrastructure.

## See Also

- [R8 / ProGuard](R8-ProGuard.md) -- R8 configuration and keep rules
- [Parameterized Types](Parameterized-Types.md) -- how parameterized types flow through serialization
