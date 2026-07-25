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

import com.squareup.kotlinpoet.NameAllocator

/**
 * Central name allocator that produces collision-safe Kotlin identifiers while
 * preserving original GraphQL wire names for serialization annotations.
 *
 * ## Per-Scope Allocation
 *
 * Create one allocator instance per scope (e.g. per class, per operation) to
 * ensure deterministic collision resolution within that scope. Each allocator
 * tracks allocated names independently, so collisions across scopes are
 * allowed (different classes may have properties with the same Kotlin name
 * but different GraphQL names).
 *
 * ## Usage
 *
 * ```kotlin
 * val allocator = GraphNameAllocator()
 * val name = allocator.allocatePropertyName("private")
 * // GeneratedName(kotlinName="privateValue", wireName="private")
 * ```
 *
 * The [GeneratedName.wireName] is **always** the original GraphQL name, set
 * before any keyword escaping or collision resolution. It is **never** derived
 * from [GeneratedName.kotlinName]. This invariant guarantees that
 * `@SerialName("wireName")` always produces the correct JSON key matching
 * the GraphQL schema, regardless of Kotlin keyword escaping or collision
 * resolution applied to `kotlinName`.
 *
 * ## Keyword Escape Policy
 *
 * Hard Kotlin keywords and visibility modifiers are escaped by appending a
 * `Value` suffix (e.g. `private` -> `privateValue`). Digits at the start
 * are prefixed with `_`. Backticks are intentionally not used. See
 * [NamePolicy] for the complete keyword list and rationale.
 *
 * Keyword pre-allocation in the underlying KotlinPoet [NameAllocator] is
 * **disabled** because [NamePolicy] handles keyword escaping before the
 * suggestion reaches the allocator. If pre-allocation were enabled,
 * KotlinPoet would pre-allocate soft keywords like `data` and `private`,
 * causing false-positive collision suffixes on escaped names.
 *
 * ## Collision Resolution
 *
 * When two distinct GraphQL names produce the same Kotlin candidate (after
 * keyword escaping), KotlinPoet's [NameAllocator] appends deterministic
 * underscore-based suffixes (`foo`, `foo_`, `foo__`). Resolution is:
 * - Scoped per allocator instance
 * - Deterministic for the same input order
 * - Applied **after** keyword escaping
 *
 * ## Wire Name Example
 *
 * ```kotlin
 * val allocator = GraphNameAllocator()
 *
 * // Keyword escaped
 * val name1 = allocator.allocatePropertyName("private")
 * assertEquals("privateValue", name1.kotlinName)
 * assertEquals("private", name1.wireName) // original preserved for @SerialName
 *
 * // Collision (same root after escaping)
 * val name2 = allocator.allocatePropertyName("myField")
 * val name3 = allocator.allocatePropertyName("my_field")
 * // name2.kotlinName != name3.kotlinName (one gets suffix)
 * // Both retain their distinct wire names for @SerialName
 * ```
 *
 * @see NamePolicy
 * @see GeneratedName
 * @since 1.0
 */
class GraphNameAllocator {
    /**
     * Keyword pre-allocation is disabled because [NamePolicy] handles keyword
     * escaping by producing idiomatic renamed identifiers before the
     * suggestion reaches this allocator.
     */
    private val allocator: NameAllocator = NameAllocator(preallocateKeywords = false)

    /**
     * Allocates a collision-safe Kotlin class name (PascalCase) for the given
     * GraphQL [graphqlName].
     *
     * @param graphqlName The original GraphQL name (e.g. type name, operation name).
     * @return A [GeneratedName] with the allocated Kotlin identifier and the
     *   original wire name.
     */
    fun allocateClassName(graphqlName: String): GeneratedName {
        val escaped = NamePolicy.escapeKeyword(graphqlName)
        val pascal = escaped.replaceFirstChar { it.uppercase() }
        val kotlinName = allocator.newName(pascal)
        return GeneratedName(kotlinName = kotlinName, wireName = graphqlName)
    }

    /**
     * Allocates a collision-safe Kotlin property name (lowerCamelCase) for the
     * given GraphQL [graphqlName].
     *
     * @param graphqlName The original GraphQL name (e.g. field name, variable name).
     * @return A [GeneratedName] with the allocated Kotlin identifier and the
     *   original wire name.
     */
    fun allocatePropertyName(graphqlName: String): GeneratedName {
        val escaped = NamePolicy.escapeKeyword(graphqlName)
        val camel = escaped.replaceFirstChar { it.lowercase() }
        val kotlinName = allocator.newName(camel)
        return GeneratedName(kotlinName = kotlinName, wireName = graphqlName)
    }

    /**
     * Allocates a collision-safe Kotlin enum constant name (SCREAMING_SNAKE_CASE)
     * for the given GraphQL enum [graphqlValue].
     *
     * @param graphqlValue The original GraphQL enum value literal (e.g. `"OPEN"`,
     *   `"open"`, `"CREATED_AT"`).
     * @return A [GeneratedName] with the allocated Kotlin identifier and the
     *   original wire name.
     */
    fun allocateEnumConstant(graphqlValue: String): GeneratedName {
        val escaped = NamePolicy.escapeKeyword(graphqlValue)
        val upper = escaped.uppercase()
        val kotlinName = allocator.newName(upper)
        return GeneratedName(kotlinName = kotlinName, wireName = graphqlValue)
    }
}
