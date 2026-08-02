# Module Reference Map

Use this map to place code before searching for a specific file.

## Modules

| Module | Role | Dokka |
| --- | --- | --- |
| `annotations` | `@GraphQuery` annotation | `https://anitrend.github.io/retrofit-graphql/` |
| `api` | Backend-neutral protocol/operation/registry contracts (`GraphQLOperation`, `GraphQLDocumentRegistry`, `GraphQLVariables`, `EmptyGraphQLVariables`, `GraphQLOperationRequest`, `GraphQLResponse`/`GraphQLData`/`GraphQLResponseError`, `GraphQLValue`, `GraphQLPathSegment`) | `https://anitrend.github.io/retrofit-graphql/` |
| `android-assets` | Android asset-based query discovery (`GraphProcessor`, `AssetManagerDiscoveryPlugin`, APQ, logging) | `https://anitrend.github.io/retrofit-graphql/` |
| `runtime` | Backend-neutral Retrofit converters (`GraphQLConverterFactory`, `GraphQLRequestConverter`, `GraphQLResponseConverter`) | `https://anitrend.github.io/retrofit-graphql/` |
| `compat` | **Deprecated** legacy implementation module: `GraphConverter`, `GraphRequestConverter`, `GraphResponseConverter`, `GraphErrorUtil`, `GraphQLRequest`, `GraphQLJson`, `GsonGraphQLJson`, `KotlinxGraphQLJson`, `GraphContainer`, `GraphError`, `QueryContainer(Builder)`, persisted-query URL types, `GraphQLResponseException`/helpers, and the 37 `io.github.wax911.library.*` type aliases | `https://anitrend.github.io/retrofit-graphql/` |
| `codegen-core` | Build-time code generation engine (graphql-java parsing + KotlinPoet generation; uses SchemaIndex, GraphQLTypeMapper, GraphQLDefaultValueRenderer, GraphQLTypeUsageValidator) | — |
| `gradle-plugin` | Gradle plugin for code generation (`id("co.anitrend.retrofit.graphql.codegen")`) | — |
| `serialization-api` | Backend-neutral transport contract (`GraphQLTransportCodec` + codec exceptions; depends on `:api` only) | `https://anitrend.github.io/retrofit-graphql/` |
| `serialization-gson` | Gson-backed transport codec (`GsonGraphQLTransportCodec` only; legacy `GsonGraphQLJson` moved to `:compat`) | `https://anitrend.github.io/retrofit-graphql/` |
| `serialization-kotlinx` | kotlinx.serialization-backed transport codec (`KotlinxGraphQLTransportCodec` only; legacy `KotlinxGraphQLJson` moved to `:compat`) | `https://anitrend.github.io/retrofit-graphql/` |
| `library` | **Deprecated** backward-compatible aggregator (`api()` re-exports, no sources; aliases ship from `:compat`) | `https://anitrend.github.io/retrofit-graphql/` |
| `app` | Sample application demonstrating the generated-request path (`GraphQLConverterFactory` + `KotlinxGraphQLTransportCodec` + `GraphQLResponse`) and the legacy asset-based bucket flow through `:compat` (`GraphConverter` + `GraphContainer`); included in CI via `-PincludeSampleApp=true` | — |

## Module Package Roots (`co.anitrend.retrofit.graphql`)

| Module | Key Packages | Use for |
| --- | --- | --- |
| `annotations` | `co.anitrend.retrofit.graphql.annotation` | `@GraphQuery` annotation |
| `api` | `co.anitrend.retrofit.graphql.model`, `co.anitrend.retrofit.graphql.model.request` | Backend-neutral contracts, request/response models |
| `runtime` | `co.anitrend.retrofit.graphql.converter`, `co.anitrend.retrofit.graphql.converter.request`, `co.anitrend.retrofit.graphql.converter.response` | Backend-neutral converter factory and converters |
| `compat` | `co.anitrend.retrofit.graphql.converter`, `co.anitrend.retrofit.graphql.converter.request`, `co.anitrend.retrofit.graphql.converter.response`, `co.anitrend.retrofit.graphql.model`, `co.anitrend.retrofit.graphql.model.body`, `co.anitrend.retrofit.graphql.model.attribute`, `co.anitrend.retrofit.graphql.model.request`, `co.anitrend.retrofit.graphql.serialization.gson`, `co.anitrend.retrofit.graphql.serialization.kotlinx`, `co.anitrend.retrofit.graphql.util`, `io.github.wax911.library.*` | Legacy converters, legacy models, legacy serialization wrappers, error utilities, type aliases |
| `android-assets` | `co.anitrend.retrofit.graphql.annotation.processor`, `co.anitrend.retrofit.graphql.annotation.processor.fragment`, `co.anitrend.retrofit.graphql.annotation.processor.plugin`, `co.anitrend.retrofit.graphql.logger`, `co.anitrend.retrofit.graphql.persistedquery` | Asset-based query discovery, fragment patching, APQ, logging |
| `codegen-core` | `co.anitrend.retrofit.graphql.codegen.parser`, `co.anitrend.retrofit.graphql.codegen.generate`, `co.anitrend.retrofit.graphql.codegen.mapping`, `co.anitrend.retrofit.graphql.codegen.model`, `co.anitrend.retrofit.graphql.codegen.naming`, `co.anitrend.retrofit.graphql.codegen.config` | GraphQL parsing, Kotlin code generation, type mapping, naming contract (`GeneratedName`, `NamePolicy`, `GraphNameAllocator`), serialization backend config (`SerializationBackend`) |
| `serialization-api` | `co.anitrend.retrofit.graphql.serialization` | Backend-neutral codec contract and codec exceptions |
| `serialization-gson` | `co.anitrend.retrofit.graphql.serialization.gson` | Gson transport codec (`GsonGraphQLTransportCodec`) |
| `serialization-kotlinx` | `co.anitrend.retrofit.graphql.serialization.kotlinx` | kotlinx.serialization transport codec (`KotlinxGraphQLTransportCodec`) |

## Placement Heuristics

- New annotation for tagging Retrofit methods: `annotations/`.
- Changes to backend-neutral protocol/operation/registry contracts: `api/`.
- Changes to the backend-neutral Retrofit converter wiring: `runtime/`.
- Changes to the deprecated legacy surface (converters, models, wrappers, aliases): `compat/`.
- Changes to how `.graphql` files are discovered or fragment resolution: `android-assets/`.
- Custom file source (network, classpath, etc.): extend `AbstractDiscoveryPlugin` in `android-assets/`.
- Build-time code generation logic: `codegen-core/`.
- Gradle plugin behavior or DSL: `gradle-plugin/`.
- Custom logging or log level filtering: extend `AbstractLogger` in `android-assets/`.
- Custom backend-neutral JSON codec: new module implementing `GraphQLTransportCodec`.

## Consumer Entry Points

Consumers typically interact with:

1. `GraphQLConverterFactory` — backend-neutral Retrofit converter factory (from `:runtime`); requires an explicit `GraphQLTransportCodec` on every creation path (`create(codec = ..., registry = ...)`), accepts `GraphQLOperationRequest` bodies only.
2. `GraphQLTransportCodec` — backend-neutral request/response codec contract (from `:serialization-api`); wraps failures in `GraphQLRequestEncodingException`/`GraphQLResponseDecodingException`.
3. `GsonGraphQLTransportCodec` / `KotlinxGraphQLTransportCodec` — built-in `GraphQLTransportCodec` implementations (from `:serialization-gson` / `:serialization-kotlinx`).
4. `GraphQLOperationRequest<TVariables>` — backend-neutral request payload (from `:api`); used by `GraphQLTransportCodec` (from `:serialization-api`). Provides `withPersistedQuery(...)`. Generated operation objects provide `.request(...)` factory methods.
5. `GraphQLResponse<T>` — backend-neutral response envelope (from `:api`); `data` modeled via `GraphQLData` (Absent vs Present(null)), errors via `GraphQLResponseError` with required non-null `message`, arbitrary JSON fields via `GraphQLValue` (`Null`, `BooleanValue`, `StringValue`, `NumberValue`, `ListValue`, `ObjectValue`), error paths via `GraphQLPathSegment` (`Field`/`Index`).
6. `GraphQLVariables` — marker interface for generated variable classes (from `:api`).
7. `EmptyGraphQLVariables` — plain (non-`@Serializable`) sentinel singleton for operations without variables (from `:api`); the legacy kotlinx wrapper special-cases it in `:compat`.
8. `GraphQLDocumentRegistry` — providing build-time generated operation documents (from `:api`, implemented by `GeneratedGraphQLRegistry` from codegen output).
9. `@GraphQuery` — placed on Retrofit interface methods (from `:annotations`); alternative to generated types for asset-based workflows.
10. `GraphConverter` — deprecated legacy converter (from `:compat`); Gson/GraphQLJson-backed, asset-based. Deprecated in favor of `GraphQLConverterFactory`.
11. `GraphContainer<T>` / `GraphError` / `GraphQLRequest<TVariables>` — deprecated legacy serializer-coupled models (from `:compat`); used by the `GraphConverter` flow and the 37 `io.github.wax911.library.*` type aliases (also from `:compat`).
12. `GraphQLJson` — deprecated legacy serialization seam (from `:compat`); implementations `GsonGraphQLJson`, `KotlinxGraphQLJson` (from `:compat`).
13. `QueryContainerBuilder` — deprecated Parcelable request builder (from `:compat`); asset-based flow.
14. `AbstractDiscoveryPlugin` — custom file discovery (from `:android-assets`).
15. `AbstractLogger` — custom logging backend (from `:android-assets`).
