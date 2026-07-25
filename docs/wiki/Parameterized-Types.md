# Parameterized Types

This page documents how retrofit-graphql handles Java `ParameterizedType` values passed from Retrofit's converter infrastructure, and how `KotlinxGraphQLJson` resolves serializers for generic types.

## Background

When Retrofit calls a converter, it passes the full Java reflection `Type` of the method's return/parameter type. For generic types, this is a `java.lang.reflect.ParameterizedType`:

```kotlin
suspend fun getCurrentUser(...): Response<GraphContainer<GetCurrentUserData>>
//                                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                                 ParameterizedType: GraphContainer<GetCurrentUserData>
```

The converter must resolve this to a concrete serializer capable of deserializing `GraphContainer<GetCurrentUserData>`, not just `GraphContainer<*>`.

## How It Works

### Flow

1. **Retrofit** passes the `ParameterizedType` to `GraphResponseConverter`
2. **GraphResponseConverter** forwards it to `GraphQLJson.decode(json, type)`
3. **GraphQLJson** implementation resolves a serializer for the full parameterized type

### KotlinxGraphQLJson

Uses `kotlinx.serialization.serializer(type: java.lang.reflect.Type)` -- a JVM reflection extension from `kotlinx-serialization-json` that handles:

- `Class<*>` -- standard class types
- `ParameterizedType` -- e.g., `GraphContainer<GetCurrentUserData>`
- `GenericArrayType`
- `WildcardType`

```kotlin
@Suppress("UNCHECKED_CAST")
private fun <T : Any> resolveSerializer(type: Type): KSerializer<T> {
    return json.serializersModule.serializer(type) as KSerializer<T>
}
```

This extension recursively resolves serializers for all type arguments. For `GraphContainer<GetCurrentUserData>`, it resolves:

1. `GraphContainer` serializer (from `@Serializable` annotation)
2. `GetCurrentUserData` serializer (from `@Serializable` annotation)
3. Recursively for any nested type parameters in `GetCurrentUserData`

### GsonGraphQLJson

Uses `Gson.getAdapter(TypeToken.get(type))` which natively supports `ParameterizedType`:

```kotlin
override fun <T : Any> decode(json: String, type: Type): T {
    return gson.fromJson(json, type)
}
```

## Why This Matters

### Before (Broken)

Prior to v0.13.x, `KotlinxGraphQLJson` attempted to resolve the serializer using only the raw class (losing type arguments):

```kotlin
// Broken: loses GetCurrentUserData type argument
val serializer = serializer<GraphContainer<*>>()
```

This failed at runtime with a `SerializationException` because kotlinx could not find a serializer for the erased type `GraphContainer<*>`.

### After (Fixed)

Using `serializer(type: java.lang.reflect.Type)`, the full `ParameterizedType` is passed through, and kotlinx can resolve the correct `KSerializer<GraphContainer<GetCurrentUserData>>`.

## Custom Backend Requirements

If you implement a custom `GraphQLJson` backend, your `decode(json, type)` must handle `ParameterizedType`:

```kotlin
class CustomGraphQLJson : GraphQLJson {
    override fun <T : Any> decode(json: String, type: Type): T {
        // type may be Class<T> or ParameterizedType (e.g., GraphContainer<Foo>)
        // Your JSON library must support resolving adapters for parameterized types
    }
}
```

Common patterns:

- **Moshi**: `moshi.adapter<T>(type).fromJson(json)`
- **Jackson**: `objectMapper.readValue(json, TypeFactory.constructType(type))`
- **Gson**: `gson.fromJson(json, type)` (passes `Type` directly)
- **Kotlinx**: `serializer(type: Type)` (JVM reflection extension)

## Request Side

The same parameterized type flow applies to request serialization via `GraphRequestConverter`:

```kotlin
private fun convertGraphQLRequest(request: GraphQLRequest<*>): RequestBody {
    val requestJson =
        if (type != null) json.encode(request, type) else json.encode(request)
    return requestJson.toRequestBody(MEDIA_TYPE)
}
```

When `type` is available (e.g., `GraphQLRequest<GetMarketPlaceAppsVariables>`), it's forwarded to `GraphQLJson.encode(value, type)` so that parameterized-aware serializers can resolve the correct serializer for the variables type parameter.

## See Also

- [Serialization Backends](Serialization-Backends.md) -- implementing custom backends that handle parameterized types
