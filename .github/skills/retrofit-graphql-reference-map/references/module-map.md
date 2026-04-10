# Module Reference Map

Use this map to place code before searching for a specific file.

## Modules

| Module | Role | Dokka |
| --- | --- | --- |
| `library` | Core converter library — all public API, annotations, converter, discovery, logger, model, and persisted-query logic | `https://anitrend.github.io/retrofit-graphql/` |
| `app` | Sample application demonstrating library integration; excluded from CI builds (requires `CI` env var to be unset) | — |

## Library Package Roots (`io.github.wax911.library`)

| Package | Contents | Use for |
| --- | --- | --- |
| `annotation` | `@GraphQuery` annotation | Marking Retrofit interface methods with their `.graphql` file name |
| `annotation.processor` | `GraphProcessor`, `AbstractGraphProcessor` | Processing annotations at request time; reading and injecting `.graphql` file content |
| `annotation.processor.fragment` | `FragmentAnalyzer`, `FragmentPatcher`, `RegexFragmentAnalyzer`, `FragmentAnalysis`, `GraphRegexUtil` | Detecting and inlining GraphQL fragment dependencies |
| `annotation.processor.plugin` | `AssetManagerDiscoveryPlugin` | Android `AssetManager`-backed file discovery (default implementation) |
| `annotation.processor.plugin.contract` | `AbstractDiscoveryPlugin` | Extension point for custom file discovery strategies |
| `converter` | `GraphConverter` | Retrofit `Converter.Factory` entry point; handles both request and response conversion |
| `converter.request` | `GraphRequestConverter` | Converts annotated method calls into GraphQL `RequestBody` |
| `converter.response` | `GraphResponseConverter` | Delegates JSON deserialization to the wrapped converter |
| `logger` | `DefaultGraphLogger` | Built-in logger implementation using Android `Log` |
| `logger.contract` | `ILogger` | Logger interface for custom logger implementations |
| `logger.core` | `AbstractLogger` | Abstract base class for custom loggers |
| `model.attribute` | `GraphError` | Error object returned by the GraphQL server |
| `model.body` | `GraphContainer` | Generic response wrapper exposing `data` and `errors` |
| `model.request` | `QueryContainer`, `QueryContainerBuilder`, `PersistedQuery`, `PersistedQueryUrlParameters`, `PersistedQueryUrlParameterBuilder` | Request body models and builders |
| `persisted.contract` | `IAutomaticPersistedQuery` | Contract for Automatic Persisted Queries (APQ) |
| `persisted.query` | `AutomaticPersistedQueryCalculator` | Hash calculator for APQ |
| `persisted.query.error` | `AutomaticPersistedQueryErrors` | APQ-specific error types |
| `util` | `GraphErrorUtil`, `Logger` | Shared utility helpers |

## Placement Heuristics

- New annotation for tagging Retrofit methods or parameters: `annotation/`.
- Changes to how `.graphql` files are read or fragment dependencies resolved: `annotation.processor/` or `annotation.processor.fragment/`.
- Custom file source (network, classpath, etc.): extend `AbstractDiscoveryPlugin` in `annotation.processor.plugin/`.
- Changes to the Retrofit factory or request/response wiring: `converter/`, `converter.request/`, or `converter.response/`.
- Custom logging or log level filtering: extend `AbstractLogger` in `logger.core/` and register via `GraphConverter`.
- New request body fields or response wrapper changes: `model.request/` or `model.body/`.
- Persisted Query / APQ changes: `persisted/` packages.
- Shared utility used across packages: `util/`.

## Consumer Entry Points

Consumers typically interact with:

1. `GraphConverter` — registered as a Retrofit converter factory.
2. `@GraphQuery` — placed on Retrofit interface methods to bind a `.graphql` file.
3. `GraphContainer<T>` — the generic response wrapper for all GraphQL responses.
4. `QueryContainerBuilder` — for manually constructing request bodies when needed.
5. `AbstractDiscoveryPlugin` — extended to provide a custom file discovery strategy.
6. `AbstractLogger` — extended to integrate a custom logging backend.
