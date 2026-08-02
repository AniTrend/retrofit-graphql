# Serialization Backends

retrofit-graphql has **two independent serialization layers** that are often confused:

1. **Code generation annotation setting** (`serializationBackend = NONE | KOTLINX | GSON`) -- controls which annotations the codegen plugin emits on *generated types*.
2. **Runtime transport codecs** (`GraphQLTransportCodec` implementations) -- control how *requests and responses are serialized on the wire* by the new `GraphQLConverterFactory`.

They are independent: you can generate plain `NONE` types and decode them with a kotlinx or Gson codec, or generate `@Serializable` types and decode them with either codec. There is also a deprecated third layer, the legacy `GraphQLJson` seam used by `GraphConverter` (shipped from `:compat`).

## Code Generation Setting (`serializationBackend`)

The codegen plugin emits annotations on generated variable classes, input objects, enums, and response models. The setting is strict: the configured value is used as-is and **never auto-selected or upgraded**.

| Value | Annotations on Generated Types | Response Models | Notes |
|-------|--------------------------------|-----------------|-------|
| `NONE` | None | Plain data holders; plain sealed interfaces for abstract types | Strict and plain: no serializer imports, annotations, or adapters, even with `generateResponses = true`. Abstract-type dispatch is then the consumer's or the codec's responsibility. |
| `KOTLINX` | `@Serializable`, `@SerialName`, `@JsonClassDiscriminator("__typename")` on sealed polymorphism roots | Supported, including interface/union paths | Requires the kotlinx.serialization compiler plugin in the consuming module. |
| `GSON` | `@SerializedName` on properties | Concrete operations only | Operations with interface/union response paths fail at codegen time with a path-aware diagnostic. |

**Backend-independent outputs**: operation constants (`GraphQLOperations`, `GraphQLDocuments`, `GraphQLHashes`) and the generated document registry (`GeneratedGraphQLRegistry`) carry no serializer types and are identical across backends. The executable documents always inject `__typename` for abstract operations, so the wire payload is the same regardless of the annotation setting.

**No backend is preferred.** Capability differences are expected: `KOTLINX` supports polymorphic response DTOs out of the box, `GSON` is concrete-only for responses, and `NONE` delegates everything to your runtime codec. Choose per target based on your model and tooling, not on a notion of a "default".

## Runtime Transport Codecs (`GraphQLTransportCodec`)

The backend-neutral `GraphQLConverterFactory` (`:runtime`) requires an explicit `GraphQLTransportCodec` on every creation path. There is **no default codec** and no reflection-based selection:

```kotlin
val factory = GraphQLConverterFactory.create(
    codec = KotlinxGraphQLTransportCodec(),   // or GsonGraphQLTransportCodec()
    registry = GeneratedGraphQLRegistry,      // optional, retained for API parity
)
```

### Built-in Codecs

| Codec | Module | Backend | Behavior |
|-------|--------|---------|----------|
| `KotlinxGraphQLTransportCodec` | `:serialization-kotlinx` | kotlinx.serialization | Compile-time serializers; `@SerialName` wire names on generated DTOs; R8-safe by default |
| `GsonGraphQLTransportCodec` | `:serialization-gson` | Gson | Reflection-based; handles all envelope fields; may need consumer-owned keep rules for reflectively deserialized DTOs |

Both codecs implement the same neutral protocol semantics:

- **Request encoding** (`encodeRequest`): writes the `GraphQLOperationRequest` envelope (`query`, `operationName`, `variables`, `extensions`). Typed variables are resolved from the parameterized request type (e.g. `GraphQLOperationRequest<GetMarketPlaceAppsVariables>`).
- **Response decoding** (`decodeResponse`): parses the `GraphQLResponse` envelope:
  - `data` missing -> `GraphQLData.Absent`; explicit `data: null` -> `GraphQLData.Present(null)`
  - `errors` entries require a non-null `message`; optional `locations` and typed `path` (`GraphQLPathSegment.Field`/`Index`)
  - `extensions` (top-level and per-error) decode to `GraphQLValue.ObjectValue` with exact `BigDecimal` precision
- **Failure policy**: every failure is wrapped in `GraphQLRequestEncodingException` or `GraphQLResponseDecodingException`; backend exceptions never leak.

### Custom Codecs

Implement `GraphQLTransportCodec` (`:serialization-api`) for any JSON framework. The codec owns:

- **Adapter resolution** for `java.lang.reflect.ParameterizedType` (e.g. `GraphQLResponse<GetCurrentUserData>`)
- **Wire names** and enum representation
- **Polymorphism** (e.g. `__typename`-based dispatch for abstract generated types, especially with `serializationBackend = NONE`)
- **Scalar mapping** and arbitrary `GraphQLValue` handling
- **R8 rules** for any reflection it relies on

```kotlin
class MoshiGraphQLTransportCodec(private val moshi: Moshi) : GraphQLTransportCodec {
    override fun encodeRequest(request: GraphQLOperationRequest<*>, requestType: Type): String =
        try {
            // resolve the typed variables adapter from the parameterized requestType
            // and write the neutral envelope
            ...
        } catch (cause: Exception) {
            throw GraphQLRequestEncodingException(
                operationName = request.operationName,
                requestType = requestType,
                message = "Failed to encode GraphQL request",
                cause = cause,
            )
        }

    override fun <T : Any> decodeResponse(body: String, responseType: Type): T =
        try {
            // parse the envelope, resolve the data adapter from responseType,
            // and build the neutral GraphQLResponse
            ...
        } catch (cause: Exception) {
            throw GraphQLResponseDecodingException(
                responseType = responseType,
                message = "Failed to decode GraphQL response",
                cause = cause,
            )
        }
}
```

Register it the same way as the built-ins: `GraphQLConverterFactory.create(codec = MoshiGraphQLTransportCodec(moshi), registry = ...)`.

## Legacy `GraphQLJson` Seam (Deprecated, `:compat`)

The deprecated `GraphConverter` path uses a pluggable `GraphQLJson` abstraction instead of the codec SPI. Both implementations ship from `:compat`:

| Backend | Class | Notes |
|---------|-------|-------|
| kotlinx.serialization | `KotlinxGraphQLJson` | Parameterized types via the JVM reflection extension; `@Transient` limitations below |
| Gson | `GsonGraphQLJson` | Handles all fields including `extensions`/`path` |

```kotlin
// Legacy (deprecated, :compat)
val converter = GraphConverter.create(
    context = context,
    json = KotlinxGraphQLJson(Json { ignoreUnknownKeys = true }),
    registry = GeneratedGraphQLRegistry,
)
```

### Legacy kotlinx Limitations

| Field | Reason |
|-------|--------|
| `GraphContainer.extensions` (`Map<Any, Any>`) | Cannot be statically resolved by compiler plugin |
| `GraphQLRequest.extensions` (`Map<String, Any?>`) | Cannot be statically resolved by compiler plugin |
| `GraphError.path` (`List<Any>`) | Cannot be statically resolved by compiler plugin |
| `GraphError.extensions` (`Map<String, Any?>`) | Cannot be statically resolved by compiler plugin |

These fields are marked `@Transient` and are excluded from kotlinx deserialization on the legacy path. Use the Gson-backed `GsonGraphQLJson` if you need them preserved exactly on both encode and decode. The neutral `GraphQLResponse` contract has no such limitation: extensions and error paths are `GraphQLValue` trees.

### Legacy APQ Behavior

`GraphQLRequest.withPersistedQuery()` works on the typed legacy kotlinx request path: `GraphQLRequest.extensions` remains `@Transient`, but `KotlinxGraphQLJson.encode()` merges supported extension values, including `PersistedQuery`, into the outgoing JSON. On the neutral path, `GraphQLOperationRequest.withPersistedQuery(...)` stores the extension structurally as `GraphQLValue.ObjectValue` and works with every codec.

## R8

- **kotlinx.serialization**: the consumer rules ship with the `kotlinx-serialization-core`/`-json` artifacts and protect every `@Serializable` class (generated DTOs, legacy `:compat` models). No library module ships keep rules for these.
- **Gson**: reflection-based; the sample app needs 2 targeted, app-owned keep rules for its upload path. Custom Gson codecs and `@SerializedName` DTOs need consumer-owned scoped rules.
- **`EmptyGraphQLVariables`**: a plain object, no rules needed.

See [R8 / ProGuard](R8-ProGuard.md) for details.

## See Also

- [R8 / ProGuard](R8-ProGuard.md) -- R8 configuration and keep rules
- [Parameterized Types](Parameterized-Types.md) -- how parameterized types flow through both serialization layers
- [Code Generation](Codegen.md) -- the `serializationBackend` codegen setting
