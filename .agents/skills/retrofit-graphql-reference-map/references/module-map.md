# Module Reference Map

Use this map to place code before searching for a specific file.

## Modules

| Module | Role | Dokka |
| --- | --- | --- |
| `annotations` | `@GraphQuery` annotation | `https://anitrend.github.io/retrofit-graphql/` |
| `api` | Public API interfaces and models (`GraphQLOperation`, `GraphQLDocumentRegistry`, `QueryContainerBuilder`, `GraphContainer`) | `https://anitrend.github.io/retrofit-graphql/` |
| `runtime` | Retrofit converter (`GraphConverter`, `GraphRequestConverter`, `GraphResponseConverter`) | `https://anitrend.github.io/retrofit-graphql/` |
| `android-assets` | Android asset-based query discovery (`GraphProcessor`, `AssetManagerDiscoveryPlugin`, APQ, logging) | `https://anitrend.github.io/retrofit-graphql/` |
| `codegen-core` | Build-time code generation engine (graphql-java parsing + KotlinPoet generation) | — |
| `gradle-plugin` | Gradle plugin for code generation (`id("co.anitrend.retrofit.graphql.codegen")`) | — |
| `serialization-gson` | Gson-backed `GraphQLJson` implementation | `https://anitrend.github.io/retrofit-graphql/` |
| `serialization-kotlinx` | kotlinx.serialization-backed `GraphQLJson` implementation | `https://anitrend.github.io/retrofit-graphql/` |
| `library` | **Deprecated** backward-compatible aggregator (type aliases, `api()` re-exports) | `https://anitrend.github.io/retrofit-graphql/` |
| `app` | Sample application demonstrating GitHub GraphQL API integration; excluded from CI (requires `CI` env var to be unset) | — |

## Module Package Roots (`co.anitrend.retrofit.graphql`)

| Module | Key Packages | Use for |
| --- | --- | --- |
| `annotations` | `co.anitrend.retrofit.graphql.annotation` | `@GraphQuery` annotation |
| `api` | `co.anitrend.retrofit.graphql.model`, `co.anitrend.retrofit.graphql.model.body`, `co.anitrend.retrofit.graphql.model.request`, `co.anitrend.retrofit.graphql.model.attribute` | Public API interfaces, request/response models |
| `runtime` | `co.anitrend.retrofit.graphql.converter`, `co.anitrend.retrofit.graphql.converter.request`, `co.anitrend.retrofit.graphql.converter.response`, `co.anitrend.retrofit.graphql.util` | Retrofit converter, request/response conversion, error utilities |
| `android-assets` | `co.anitrend.retrofit.graphql.annotation.processor`, `co.anitrend.retrofit.graphql.annotation.processor.fragment`, `co.anitrend.retrofit.graphql.annotation.processor.plugin`, `co.anitrend.retrofit.graphql.logger`, `co.anitrend.retrofit.graphql.persistedquery` | Asset-based query discovery, fragment patching, APQ, logging |
| `codegen-core` | `co.anitrend.retrofit.graphql.codegen.parser`, `co.anitrend.retrofit.graphql.codegen.generate`, `co.anitrend.retrofit.graphql.codegen.mapping`, `co.anitrend.retrofit.graphql.codegen.model` | GraphQL parsing, Kotlin code generation, type mapping |
| `serialization-gson` | `co.anitrend.retrofit.graphql.serialization.gson` | Gson JSON serialization |
| `serialization-kotlinx` | `co.anitrend.retrofit.graphql.serialization.kotlinx` | kotlinx.serialization JSON backend |

## Placement Heuristics

- New annotation for tagging Retrofit methods: `annotations/`.
- Changes to public API interfaces or request/response models: `api/`.
- Changes to Retrofit converter wiring: `runtime/`.
- Changes to how `.graphql` files are discovered or fragment resolution: `android-assets/`.
- Custom file source (network, classpath, etc.): extend `AbstractDiscoveryPlugin` in `android-assets/`.
- Build-time code generation logic: `codegen-core/`.
- Gradle plugin behavior or DSL: `gradle-plugin/`.
- Custom logging or log level filtering: extend `AbstractLogger` in `android-assets/`.
- Custom JSON serialization backend: new module implementing `GraphQLJson`.

## Consumer Entry Points

Consumers typically interact with:

1. `GraphConverter` — registered as a Retrofit converter factory (from `:runtime`).
2. `@GraphQuery` — placed on Retrofit interface methods (from `:annotations`).
3. `GraphContainer<T>` — generic response wrapper (from `:api`).
4. `QueryContainerBuilder` — manually constructing request bodies (from `:api`).
5. `GraphQLDocumentRegistry` — providing build-time generated operation documents (from `:api`, used with `:codegen-core` / `:gradle-plugin`).
6. `AbstractDiscoveryPlugin` — custom file discovery (from `:android-assets`).
7. `AbstractLogger` — custom logging backend (from `:android-assets`).
