# Sample Application

This sample demonstrates retrofit-graphql with build-time code generated types, the backend-neutral converter path, a legacy compatibility flow, Gson for multipart uploads, and R8 in release builds.

## Architecture

### Serialization Strategy (Dual Wiring)

The sample intentionally demonstrates **both** converter paths, one per endpoint:

| Endpoint | Converter | Codec / Serializer | Contracts |
|----------|-----------|--------------------|-----------|
| GitHub API (market, user) | `GraphQLConverterFactory` (`:runtime`) | `KotlinxGraphQLTransportCodec` (`:serialization-kotlinx`) | `GraphQLOperationRequest` + `GraphQLResponse`/`GraphQLData` (neutral) |
| Bucket API (asset-based, uploads) | `GraphConverter` (`:compat`, deprecated legacy) | `KotlinxGraphQLJson` (`:compat`) | `GraphQLRequest` + `GraphContainer` (legacy) |
| Multipart upload payloads | Gson, directly in `UploadMutationHelper` | `Gson` (app dependency) | operations JSON + file map |

The GitHub endpoints demonstrate the new generated-request path: `GraphQLConverterFactory` requires an explicit `GraphQLTransportCodec`, accepts `GraphQLOperationRequest` bodies only, and decodes the neutral `GraphQLResponse` envelope (with `GraphQLData.Absent` vs `Present(null)`, typed error paths, and `GraphQLValue` extensions). The bucket endpoints keep the deprecated `GraphConverter`/`GraphContainer` flow through `:compat` as the legacy asset-based demonstration.

### Codegen Configuration

The codegen plugin generates typed response DTOs, variable classes, enums, and request helpers for the GitHub API schema:

```kotlin
// app/build.gradle.kts
retrofitGraphQL {
    common {
        generateVariables.set(true)
        generateResponses.set(true)
        serializationBackend.set(SerializationBackend.KOTLINX)
        // serializationBackend is used as-is: NONE stays plain, no auto-selection
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

The Retrofit builder factory is parameterized by endpoint type (`data/arch/koin/Modules.kt`): GitHub gets the explicit-codec factory, bucket keeps the legacy converter:

```kotlin
factory { (endpointType: EndpointType) ->
    val registry = get<GraphQLDocumentRegistry>()

    val converterFactory =
        if (endpointType == EndpointType.GITHUB) {
            // Generated-request path: backend-neutral explicit codec.
            GraphQLConverterFactory.create(
                codec = KotlinxGraphQLTransportCodec(
                    Json { ignoreUnknownKeys = true; encodeDefaults = false }
                ),
                registry = registry,
            )
        } else {
            // Bucket asset-based demonstration: legacy GraphConverter via :compat.
            val level = if (BuildConfig.DEBUG)
                ILogger.Level.VERBOSE
            else
                ILogger.Level.ERROR

            val graphQLJson = KotlinxGraphQLJson(
                Json { ignoreUnknownKeys = true; encodeDefaults = false }
            )
            GraphConverter.create(
                context = androidContext(),
                json = graphQLJson,
                registry = registry,
                level = level,
            )
        }

    Retrofit.Builder()
        .addConverterFactory(RequestBodyPassThroughConverterFactory)
        .addConverterFactory(converterFactory)
}
```

### Generated Response DTO Usage

GitHub endpoints use the neutral contracts (see `data/user/datasource/remote/UserRemoteSource.kt` and `data/market/datasource/remote/MarketPlaceRemoteSource.kt`):

```kotlin
// Retrofit interface (generated-request path)
@POST("graphql")
suspend fun getCurrentUser(
    @Body request: GraphQLOperationRequest<EmptyGraphQLVariables>,
): Response<GraphQLResponse<GetCurrentUserData>>

// With variables
@POST("graphql")
suspend fun getMarketPlaceApps(
    @Body request: GraphQLOperationRequest<GetMarketPlaceAppsVariables>,
): Response<GraphQLResponse<GetMarketPlaceAppsData>>
```

The controller layer adapts both envelopes (`SampleEnvelope.Legacy` for `GraphContainer`, `SampleEnvelope.Neutral` for `GraphQLResponse`) so market, user, and bucket flows share one pipeline.

### Multipart Upload (Gson Path)

The upload path uses Gson for serializing the operations payload because the upload mutation uses a separate bucket schema and multipart body format. Two app-owned R8 keep rules protect the Gson-serialized classes.

See `app/proguard-rules.pro` and `bucket/UploadMutationHelper.kt`.

## Dependencies

The app depends on the neutral modules plus `:compat` for the legacy bucket flow (see `app/build.gradle.kts`):

```kotlin
implementation(project(":runtime"))               // GraphQLConverterFactory
implementation(project(":api"))                    // GraphQLOperationRequest, GraphQLResponse
implementation(project(":compat"))                 // legacy GraphConverter/GraphContainer/GraphQLRequest/KotlinxGraphQLJson
implementation(project(":serialization-kotlinx"))  // KotlinxGraphQLTransportCodec
```

Gson is available to the app directly (shared dependency strategy); it is used only by the upload helper, not by the converter path.

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

Two app-owned keep rules in `proguard-rules.pro` for the Gson upload path (`GraphQLRequest`, `UploadToStorageBucketVariables`). Generated `@Serializable` types and the legacy `@Serializable` `:compat` models are protected automatically by the kotlinx.serialization artifact consumer rules. The neutral `GraphQLOperationRequest` is deliberately not kept: `verifyReleaseMapping` asserts R8 renames it, proving the generated-request path uses no reflection.

## Running Tests

### Unit Tests (Debug)

```bash
./gradlew :app:testDebugUnitTest
```

### Release-variant JVM Unit Tests

```bash
./gradlew :app:testReleaseUnitTest
```

These tests run on the JVM against release-variant classes. They do not execute
the minified R8 APK.

### Release R8 Verification

```bash
./gradlew :app:releaseR8Verification -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

This is the sample app's R8 runtime gate. It runs mapping checks and the
managed-device `pixel2api30ReleaseAndroidTest` task against the minified APK.

### All Tests

```bash
./gradlew :app:testReleaseUnitTest :app:releaseR8Verification -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

## Test Files

| Test | Location | Purpose |
|------|----------|---------|
| `ResponseDecodingTest` | `app/src/test/` | Validates the legacy `KotlinxGraphQLJson.decode` (`:compat`) with generated DTOs through `GraphContainer` |
| `MapperTest` | `app/src/test/` | Validates response DTO-to-entity mapping |
| `ReleaseSerializationTest` | `app/src/androidTest/` | Validates serialization against R8-optimized APK on device |
