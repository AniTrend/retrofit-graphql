# Parameterized Types

This page documents how retrofit-graphql handles Java `ParameterizedType` values passed from Retrofit's converter infrastructure, across both serialization layers: the legacy `GraphQLJson` seam (`:compat`) and the new `GraphQLTransportCodec` SPI (`:serialization-api`).

## Background

When Retrofit calls a converter, it passes the full Java reflection `Type` of the method's return/parameter type. For generic types, this is a `java.lang.reflect.ParameterizedType`:

```kotlin
suspend fun getCurrentUser(
    @Body request: GraphQLOperationRequest<EmptyGraphQLVariables>,
): Response<GraphQLResponse<GetCurrentUserData>>
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//         ParameterizedType: GraphQLResponse<GetCurrentUserData>
```

The converter must resolve this to a concrete serializer capable of deserializing `GraphQLResponse<GetCurrentUserData>`, not just `GraphQLResponse<*>`.

## New Layer: GraphQLTransportCodec (`:serialization-api`)

The backend-neutral `GraphQLRequestConverter`/`GraphQLResponseConverter` forward the full parameterized `Type` to the codec:

### Flow

1. **Retrofit** passes the `ParameterizedType` to `GraphQLResponseConverter`
2. **GraphQLResponseConverter** forwards it to `GraphQLTransportCodec.decodeResponse(body, responseType)`
3. The **codec** resolves a serializer/adapter for the full parameterized type

### Built-in Codecs

- **KotlinxGraphQLTransportCodec** uses `kotlinx.serialization.serializer(type: java.lang.reflect.Type)` -- a JVM reflection extension from `kotlinx-serialization-json` that handles `Class<*>`, `ParameterizedType`, `GenericArrayType`, and `WildcardType`, recursively resolving serializers for all type arguments.
- **GsonGraphQLTransportCodec** uses `Gson.getAdapter(TypeToken.get(type))` (via `fromJson`), which natively supports `ParameterizedType`.

The same flow applies on the request side: `GraphQLRequestConverter` forwards the body parameter's `Type` (e.g. `GraphQLOperationRequest<GetMarketPlaceAppsVariables>`) to `GraphQLTransportCodec.encodeRequest(request, requestType)` so the codec can resolve the typed variables serializer from the declared generic argument rather than the runtime class.

## Legacy Layer: GraphQLJson (`:compat`, deprecated)

The deprecated `GraphResponseConverter`/`GraphRequestConverter` forward the same `ParameterizedType` to `GraphQLJson.decode(json, type)` / `encode(value, type)`:

- **KotlinxGraphQLJson** uses `kotlinx.serialization.serializer(type)` (JVM reflection extension). For `GraphContainer<GetCurrentUserData>` it resolves the `GraphContainer` serializer, the `GetCurrentUserData` serializer, and recursively any nested type parameters.
- **GsonGraphQLJson** uses `Gson.getAdapter(TypeToken.get(type))`, which natively supports `ParameterizedType`.

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

### Custom GraphQLTransportCodec (new SPI)

Your `decodeResponse(body, responseType)` must resolve adapters for `ParameterizedType`:

```kotlin
class CustomGraphQLTransportCodec : GraphQLTransportCodec {
    override fun <T : Any> decodeResponse(body: String, responseType: Type): T {
        // responseType may be Class<T> or ParameterizedType (e.g., GraphQLResponse<Foo>)
        // Your JSON library must support resolving adapters for parameterized types.
        // Failures must be wrapped in GraphQLResponseDecodingException.
    }
    override fun encodeRequest(request: GraphQLOperationRequest<*>, requestType: Type): String {
        // requestType may be ParameterizedType (e.g., GraphQLOperationRequest<FooVariables>)
        // Failures must be wrapped in GraphQLRequestEncodingException.
    }
}
```

Common patterns:

- **Moshi**: `moshi.adapter<T>(type).fromJson(json)`
- **Jackson**: `objectMapper.readValue(json, TypeFactory.constructType(type))`
- **Gson**: `gson.fromJson(json, type)` (passes `Type` directly)
- **Kotlinx**: `serializer(type: Type)` (JVM reflection extension)

### Custom GraphQLJson (legacy seam, deprecated)

The same requirement applies to `decode(json, type)` on the legacy seam: `type` may be `Class<T>` or `ParameterizedType` (e.g., `GraphContainer<Foo>`).

## See Also

- [Serialization Backends](Serialization-Backends.md) -- the two serialization layers and custom backend contracts
