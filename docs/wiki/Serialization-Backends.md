# Serialization Backends

retrofit-graphql uses a pluggable `GraphQLJson` abstraction for request and response body serialization. Two implementations are provided, and consumers can implement custom backends.

## Built-in Backends

| Backend | Module | Annotation on Generated Types | Response Models | R8 Safety |
|---------|--------|------------------------------|----------------|-----------|
| kotlinx.serialization | `:serialization-kotlinx` | `@Serializable`, `@SerialName` | Supported | Automatic |
| Gson | `:serialization-gson` | `@SerializedName` | Not supported | Keep rules needed |

## kotlinx.serialization (Recommended)

### Setup

```kotlin
dependencies {
    implementation("com.github.AniTrend.retrofit-graphql:runtime:{tag}")
    implementation("com.github.AniTrend.retrofit-graphql:serialization-kotlinx:{tag}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
```

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
@SerialName("GetCurrentUserData")
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
- `QueryContainerBuilder` (legacy builder flow stays on Gson internally)
- Multipart upload payloads

### Limitations

| Limitation | Detail |
|-----------|--------|
| `generateResponses = true` | Not supported. Gson cannot deserialize polymorphic sealed interfaces. |
| R8 | Reflection-based; may need keep rules for serialized classes. |

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

If your project uses Gson for the entire path (no kotlinx), you may need broader rules.

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
