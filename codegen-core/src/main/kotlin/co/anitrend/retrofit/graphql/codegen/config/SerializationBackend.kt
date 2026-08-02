/**
 * Copyright 2026 AniTrend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.anitrend.retrofit.graphql.codegen.config

/**
 * Selects which serialization annotations the code generator emits.
 *
 * ## Backend Behavior
 *
 * ### NONE
 * No serialization annotations emitted. Generated classes remain plain data
 * holders. Useful for consumers who bring their own serialization or who only
 * need the operation constants, registry, and variable types without JSON
 * serialization metadata.
 *
 * `NONE` may be combined with `generateResponses = true`: response models,
 * including sealed interfaces for abstract (interface/union) types, are
 * emitted as plain Kotlin structures with no serializer imports,
 * annotations, or adapters. `NONE` is never upgraded to another backend.
 *
 * ### KOTLINX
 * Emits `@Serializable` on classes and sealed interfaces, `@SerialName` on
 * properties and enum constants, and `@JsonClassDiscriminator("__typename")`
 * on sealed polymorphism roots. It is an optional backend for response model
 * generation. kotlinx.serialization is R8-safe by default (no custom keep
 * rules needed for generated types).
 *
 * ### GSON
 * Emits `@SerializedName` on properties and enum constants. Gson does not
 * require a class-level annotation, so none is emitted. Gson uses runtime
 * reflection; R8 keep rules may be needed depending on usage.
 *
 * **Limitation**: Gson cannot deserialize polymorphic sealed interfaces,
 * which are needed for GraphQL union and interface response types.
 * Using `GSON` with `generateResponses = true` is only supported for
 * operations whose response types are concrete (no interfaces or unions).
 *
 * ## DSL Integration
 *
 * Configure via the Gradle DSL:
 * ```kotlin
 * retrofitGraphQL {
 *     common {
 *         serializationBackend.set(SerializationBackend.KOTLINX)
 *     }
 * }
 * ```
 *
 * See also: `RetrofitGraphQLExtension.serializationBackend` (DSL property).
 *
 * @since 1.0
 */
enum class SerializationBackend {
    /** No serialization annotations emitted. */
    NONE,

    /** kotlinx.serialization annotations (`@Serializable`, `@SerialName`). */
    KOTLINX,

    /** Gson annotations (`@SerializedName`). */
    GSON,
}
