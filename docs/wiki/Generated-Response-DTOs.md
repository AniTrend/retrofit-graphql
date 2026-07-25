# Generated Response DTOs

When `generateResponses = true` is set in the codegen DSL, the plugin generates response model data classes for each operation's selection set. `KOTLINX` supports concrete, interface, and union response paths. `GSON` supports concrete-only response paths.

## Enabling

```kotlin
retrofitGraphQL {
    common {
        generateResponses.set(true)      // required
        // serializationBackend auto-selects KOTLINX when set to NONE (default)
    }
    target("github") {
        schema.set(file("src/main/graphql/schema.graphql"))
        operations.from(fileTree("src/main/graphql"))
    }
}
```

## Generated Output

For each `.graphql` operation, the plugin produces a `{OperationName}Data` root class with nested types for every selected GraphQL object field. The output goes to `build/generated/source/graphql/{targetName}/`.

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

Declare `GraphContainer<GeneratedData>` as the Retrofit return type:

```kotlin
internal interface UserRemoteSource {
    @POST("graphql")
    suspend fun getCurrentUser(
        @Body request: GraphQLRequest<EmptyGraphQLVariables>,
    ): Response<GraphContainer<GetCurrentUserData>>
}

internal interface MarketPlaceRemoteSource {
    @POST("graphql")
    suspend fun getMarketPlaceApps(
        @Body request: GraphQLRequest<GetMarketPlaceAppsVariables>,
    ): Response<GraphContainer<GetMarketPlaceAppsData>>
}
```

### Manual Request Construction

For operations without variables, construct the request manually using generated constants:

```kotlin
GraphQLRequest<EmptyGraphQLVariables>(
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

GraphQL interfaces and unions generate sealed interfaces with `__typename`-based polymorphism:

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

## Serialization Backend

Generated response DTOs use kotlinx.serialization annotations (`@Serializable`, `@SerialName`). The required dependency is:

```kotlin
implementation("com.github.AniTrend.retrofit-graphql:serialization-kotlinx:{tag}")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
```

Gson is supported for response models only when the operation response selection contains concrete object types. Those DTOs use `@SerializedName` on generated properties and are covered by codegen functional tests. Operations that select GraphQL interfaces or unions generate polymorphic sealed interfaces, which Gson cannot deserialize, so the codegen task fails before writing output and reports the operation name plus the exact abstract response path. Use `KOTLINX` for interface or union response DTOs.

## See Also

- [Code Generation](Codegen.md) -- plugin setup and DSL configuration
- [Serialization Backends](Serialization-Backends.md) -- kotlinx and Gson backend details
