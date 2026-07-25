# Sample Application

This sample demonstrates retrofit-graphql with build-time code generated types, kotlinx.serialization for response DTOs, Gson for multipart uploads, and R8 in release builds.

## Architecture

### Serialization Strategy

The sample uses **kotlinx.serialization** for the Retrofit response path and **Gson** only for the multipart upload path:

| Concern | Backend | Module |
|---------|---------|--------|
| GitHub API responses | kotlinx.serialization | `:serialization-kotlinx` |
| GitHub API requests | kotlinx.serialization | `:serialization-kotlinx` |
| Multipart upload payloads | Gson | Bundled via `:runtime` |
| Generated response DTOs | kotlinx.serialization | `@Serializable` from codegen |

### Codegen Configuration

The codegen plugin generates typed response DTOs, variable classes, enums, and request helpers for the GitHub API schema:

```kotlin
// app/build.gradle.kts
retrofitGraphQL {
    common {
        generateVariables.set(true)
        generateResponses.set(true)
        serializationBackend.set(SerializationBackend.KOTLINX)
        // serializationBackend auto-selects KOTLINX when generateResponses=true
    }
    packageName.set("co.anitrend.retrofit.graphql.sample.generated")
    schema.set(file("src/main/graphql/schema.graphql"))
    operations.from(fileTree("src/main/graphql") {
        include("**/*.graphql")
        exclude("**/bucket/**")  // separate schema
    })
    scalars {
        map("DateTime", "kotlin.String")
        map("GitObjectID", "kotlin.String")
        map("URI", "kotlin.String")
        map("Upload", "kotlin.String")
    }
}
```

### Registry Composition

The sample composes two registries:

1. **GeneratedGraphQLRegistry** -- codegen output for GitHub API operations
2. **BucketGraphQLRegistry** -- manually maintained for bucket uploads (separate schema)

```kotlin
single<GraphQLDocumentRegistry> {
    CompositeGraphQLRegistry(
        GeneratedGraphQLRegistry,
        BucketGraphQLRegistry,
    )
}
```

### Converter Wiring

```kotlin
factory {
    val graphQLJson = KotlinxGraphQLJson(
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
    )
    Retrofit.Builder()
        .addConverterFactory(RequestBodyPassThroughConverterFactory)
        .addConverterFactory(
            GraphConverter.create(
                context = androidContext(),
                json = graphQLJson,
                registry = get<GraphQLDocumentRegistry>(),
                level = level,
            )
        )
}
```

### Generated Response DTO Usage

```kotlin
// Retrofit interface
@POST("graphql")
suspend fun getCurrentUser(
    @Body request: GraphQLRequest<EmptyGraphQLVariables>,
): Response<GraphContainer<GetCurrentUserData>>

// With variables
@POST("graphql")
suspend fun getMarketPlaceApps(
    @Body request: GraphQLRequest<GetMarketPlaceAppsVariables>,
): Response<GraphContainer<GetMarketPlaceAppsData>>
```

### Multipart Upload (Gson Path)

The upload path uses Gson for serializing the operations payload because the upload mutation uses a separate bucket schema. Two R8 keep rules protect the Gson-serialized classes.

See `app/proguard-rules.pro` and `bucket/UploadMutationHelper.kt`.

## GraphQL Files

| Location | Purpose |
|----------|---------|
| `src/main/graphql/schema.graphql` | GitHub API schema for codegen |
| `src/main/graphql/queries/` | GitHub API queries |
| `src/main/graphql/mutations/bucket/` | Bucket upload mutation (excluded from codegen) |
| `src/main/graphql/fragments/` | Shared GraphQL fragments |

## R8 Configuration

```kotlin
android {
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
    testBuildType = "release"
}
```

Two keep rules in `proguard-rules.pro` for the Gson upload path. Generated types and `@Serializable` API types are protected automatically by kotlinx consumer rules.

## Running Tests

### Unit Tests (Debug)

```bash
./gradlew :app:testDebugUnitTest
```

### Unit Tests (Release -- R8-optimized)

```bash
./gradlew :app:testReleaseUnitTest
```

### Instrumented Tests (Release -- R8-optimized on device)

```bash
./gradlew :app:connectedReleaseAndroidTest
```

### All Tests

```bash
./gradlew :app:testReleaseUnitTest :app:connectedReleaseAndroidTest
```

## Test Files

| Test | Location | Purpose |
|------|----------|---------|
| `ResponseDecodingTest` | `app/src/test/` | Validates `KotlinxGraphQLJson.decode` with generated DTOs |
| `MapperTest` | `app/src/test/` | Validates response DTO-to-entity mapping |
| `ReleaseSerializationTest` | `app/src/androidTest/` | Validates serialization against R8-optimized APK on device |
