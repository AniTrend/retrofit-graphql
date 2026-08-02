# Generated Response DTOs

When `generateResponses = true` is set in the codegen DSL, the plugin generates response model data classes for each operation's selection set, annotated per the configured `serializationBackend`. `KOTLINX` supports concrete, interface, and union response paths. `GSON` supports concrete-only response paths. `NONE` emits plain, unannotated data holders (and plain sealed interfaces for abstract types) with no auto-selection.

## Enabling

```kotlin
retrofitGraphQL {
    common {
        generateResponses.set(true)      // required
        // serializationBackend is used as-is: NONE stays plain, no auto-selection
        serializationBackend.set(SerializationBackend.KOTLINX)  // or GSON or NONE
    }
    target("github") {
        schema.set(file("src/main/graphql/schema.graphql"))
        operations.from(fileTree("src/main/graphql"))
    }
}
```

## Generated Output

For each `.graphql` operation, the plugin produces a `{OperationName}Data` root class with nested types for every selected GraphQL object field. The output goes to `build/generated/source/graphql/{targetName}/`. The examples below show the `KOTLINX` flavor; `GSON` emits `@SerializedName` instead, and `NONE` emits the same shapes with no annotations at all.

### Example: Simple Query

Input (`queries/user/GetCurrentUser.graphql`):

```graphql
query GetCurrentUser {
    viewer {
        ...UserCore
    }
}
```

Where `UserCore` fragment references `login`, `name`, `bio`, and `status` fields.

Generated output (simplified):

```kotlin
@Serializable
data class GetCurrentUserData(
    @SerialName("viewer")
    val viewer: Viewer?,
) {
    @Serializable
    data class Viewer(
        @SerialName("id")
        val id: String,
        @SerialName("login")
        val login: String,
        @SerialName("name")
        val name: String?,
        @SerialName("bio")
        val bio: String?,
        @SerialName("status")
        val status: ViewerStatus?,
    ) {
        @Serializable
        data class ViewerStatus(
            @SerialName("message")
            val message: String?,
        )
    }
}
```

### Example: Query with Variables

Input (`queries/market/GetMarketPlaceApps.graphql`):

```graphql
query GetMarketPlaceApps($first: Int, $after: String, $before: String) {
    marketplaceListings(first: $first, after: $after, before: $before) {
        edges {
            cursor
            node { ...MarketPlaceListingCore }
        }
        pageInfo { ...PageInfo }
        totalCount
    }
}
```

Generated output includes the root `GetMarketPlaceAppsData` class with nested `MarketplaceListings`, `MarketplaceListingsEdges`, `MarketplaceListingsEdgesNode`, etc. Classes use their default fully qualified kotlinx descriptor names, while properties and enum entries carry wire-name annotations.

## Using in Retrofit

Declare the neutral contracts as the Retrofit return and body types:

```kotlin
internal interface UserRemoteSource {
    @POST("graphql")
    suspend fun getCurrentUser(
        @Body request: GraphQLOperationRequest<EmptyGraphQLVariables>,
    ): Response<GraphQLResponse<GetCurrentUserData>>
}

internal interface MarketPlaceRemoteSource {
    @POST("graphql")
    suspend fun getMarketPlaceApps(
        @Body request: GraphQLOperationRequest<GetMarketPlaceAppsVariables>,
    ): Response<GraphQLResponse<GetMarketPlaceAppsData>>
}
```

Register the explicit-codec factory on Retrofit:

```kotlin
Retrofit.Builder()
    .addConverterFactory(
        GraphQLConverterFactory.create(
            codec = KotlinxGraphQLTransportCodec(),   // or GsonGraphQLTransportCodec()
            registry = GeneratedGraphQLRegistry,
        )
    )
    .build()
```

The neutral response envelope distinguishes a missing `data` entry (`GraphQLData.Absent`) from an explicit `data: null` (`GraphQLData.Present(null)`); error `message` is required and non-null, error `path` is typed (`GraphQLPathSegment.Field`/`Index`), and `extensions` are `GraphQLValue.ObjectValue` trees.

### Manual Request Construction

For operations without variables, construct the request manually using generated constants:

```kotlin
GraphQLOperationRequest<EmptyGraphQLVariables>(
    query = GetCurrentUser.document,
    operationName = GetCurrentUser.name,
)
```

### Generated Request Factory

For operations with variables, use the generated `.request(...)` factory:

```kotlin
GetMarketPlaceApps.request(
    first = 15,
    after = null,
    before = null,
)
```

## Mapping to Domain Models

Generated types are **transport DTOs**. Map them to your domain models at the Retrofit boundary:

```kotlin
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

## Features

### Alias Preservation

When a GraphQL query uses aliases, generated properties preserve the alias name in `@SerialName`:

```graphql
query GetCurrentUserWithAliases {
    viewer {
        userId: id
        userName: name
    }
}
```

Generates `userId` and `userName` properties with `@SerialName("userId")` and `@SerialName("userName")`.

### List Nullability

Both container nullability and element nullability are preserved:

```kotlin
val edges: List<Edge>?        // nullable container, non-null elements
val edges: List<Edge?>?        // nullable container, nullable elements
val edges: List<Edge>          // non-null container, non-null elements
```

### Conditional Fields

Fields with `@include` / `@skip` directives are generated as nullable with `null` defaults:

```kotlin
@SerialName("bio")
val bio: String? = null,       // conditional field
```

### Polymorphism

GraphQL interfaces and unions generate sealed interfaces. With `KOTLINX`, dispatch uses `__typename`-based polymorphism:

```kotlin
@Serializable
@JsonClassDiscriminator("__typename")
sealed interface SearchResult {
    @Serializable
    @SerialName("User")
    data class UserValue(...) : SearchResult
    @Serializable
    @SerialName("Repository")
    data class RepositoryValue(...) : SearchResult
}
```

No manual `Json` configuration is needed. `@JsonClassDiscriminator("__typename")` handles dispatch automatically.

With `NONE`, the same shape is emitted as a plain sealed interface with no annotations, serializers, or adapters. The executable document still injects `__typename` on the wire, so a polymorphic-capable runtime codec (or your own dispatch logic) can reconstruct the concrete subtype:

```kotlin
// NONE output: plain sealed structure, no annotations
sealed interface SearchResult {
    data class UserValue(...) : SearchResult
    data class RepositoryValue(...) : SearchResult
}
```

With `GSON`, abstract response paths fail at codegen time with a path-aware diagnostic; use `KOTLINX` (or `NONE` plus a polymorphic codec) for interface or union response DTOs.

## Serialization Backend

Generated response DTOs carry annotations per the configured `serializationBackend` (`NONE`/`KOTLINX`/`GSON`). The kotlinx dependencies are only required when the generated types use them:

```kotlin
// Only when serializationBackend = KOTLINX:
implementation("com.github.AniTrend.retrofit-graphql:serialization-kotlinx:{tag}")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
```

The public `:api` contracts are backend-neutral and require no serializer dependency. `NONE` needs no serializer at all beyond the runtime codec you register on `GraphQLConverterFactory`.

## See Also

- [Code Generation](Codegen.md) -- plugin setup and DSL configuration
- [Serialization Backends](Serialization-Backends.md) -- kotlinx and Gson backend details
