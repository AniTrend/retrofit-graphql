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

package co.anitrend.retrofit.graphql.codegen.naming

/**
 * Holds both the Kotlin identifier and its corresponding wire-format name
 * after allocation through [GraphNameAllocator].
 *
 * ## Two-Name Model
 *
 * Each generated name carries two representations:
 *
 * 1. **[kotlinName]** — The collision-safe, keyword-escaped Kotlin
 *    identifier used in generated source code (e.g. `privateValue`,
 *    `isActive`, `OPEN`). This is what appears as the property name,
 *    class name, or enum constant in generated `.kt` files.
 *
 * 2. **[wireName]** — The **original** GraphQL name, never transformed.
 *    Always set before keyword escaping or collision resolution. Never
 *    derived from [kotlinName]. This is what gets written to
 *    `@SerialName("wireName")` or `@SerializedName("wireName")`, ensuring
 *    correct JSON field mapping regardless of Kotlin renaming.
 *
 * ## Invariant
 *
 * ```kotlin
 * // wireName is always the original -- never derived from kotlinName
 * val name = allocator.allocatePropertyName("private")
 * name.wireName == "private"    // always true
 * name.kotlinName == "privateValue"  // keyword-escaped for Kotlin
 * ```
 *
 * @property kotlinName The collision-safe Kotlin identifier suitable for use
 *   in generated code (e.g. `privateValue`, `isActive`, `OPEN`).
 * @property wireName The original GraphQL name preserved for use in
 *   serialization annotations (`@SerialName`, `@SerializedName`).
 * @see GraphNameAllocator
 * @see NamePolicy
 * @since 1.0
 */
data class GeneratedName(
    val kotlinName: String,
    val wireName: String,
)
