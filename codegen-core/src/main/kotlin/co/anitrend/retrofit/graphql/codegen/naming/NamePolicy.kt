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
 * Documents the keyword escaping and identifier normalisation rules used by
 * [GraphNameAllocator] when transforming GraphQL names into Kotlin identifiers.
 *
 * ## Keyword Escaping Policy
 *
 * ### Hard keywords (always escaped)
 *
 * The full set of Kotlin **hard keywords** cannot be used as identifiers in
 * any context. When a GraphQL name matches a hard keyword, the allocator
 * appends the suffix `Value` to produce a readable renamed identifier:
 *
 * - `private`  -> `privateValue`
 * - `object`   -> `objectValue`
 * - `when`     -> `whenValue`
 * - `is`       -> `isValue`
 * - `class`    -> `classValue`
 * - `fun`      -> `funValue`
 *
 * The **original** wire name is preserved separately in
 * [GeneratedName.wireName][co.anitrend.retrofit.graphql.codegen.naming.GeneratedName.wireName]
 * and used for `@SerialName` / `@SerializedName` annotations. This means
 * the JSON key always matches the GraphQL schema, regardless of Kotlin
 * keyword escaping.
 *
 * Backticks are intentionally **not** used because they degrade readability
 * in generated source code that consumers are expected to inspect directly.
 *
 * ### Visibility modifiers (escaped as soft keywords)
 *
 * The visibility modifiers `private`, `protected`, `public`, and `internal`
 * are also escaped with the `Value` suffix. While these are technically soft
 * keywords (valid as property names in data class member positions), they
 * cause compilation ambiguity when used alongside actual visibility
 * declarations. For safety, they are treated as reserved.
 *
 * ### Digits at start
 *
 * GraphQL names that start with a digit are prefixed with an underscore
 * (e.g. `3dModel` -> `_3dModel`).
 *
 * ### Soft keywords (not escaped)
 *
 * Other soft keywords (e.g. `actual`, `data`, `inner`, `sealed`, `open`)
 * are **not** escaped because they are valid identifiers in most generated
 * contexts (property names, parameter names, class names). If a consumer
 * encounters a problem with a specific soft keyword in a specific context,
 * file an issue and the policy will be extended.
 *
 * ## Collision Resolution
 *
 * When two distinct GraphQL names normalise to the same Kotlin identifier
 * candidate, [GraphNameAllocator] delegates to KotlinPoet's
 * `NameAllocator` which appends a deterministic underscore-based suffix
 * (e.g. `foo`, `foo_`, `foo__`). This resolution is:
 * - Scoped per allocator instance (per class, per operation, etc.)
 * - Deterministic for the same input and same allocation order
 * - Applied **after** keyword escaping
 *
 * ## Case conventions
 *
 * - **Property names**: lowerCamelCase (the escaped name's first character
 *   is lowercased; the rest is preserved as-is from the GraphQL name).
 * - **Class names**: PascalCase (the escaped name's first character is
 *   uppercased).
 * - **Enum constants**: SCREAMING_SNAKE_CASE (the escaped name is
 *   uppercased in its entirety).
 *
 * @see GraphNameAllocator
 * @see GeneratedName
 * @since 1.0
 */
object NamePolicy {
    /**
     * Kotlin **hard keywords** that cannot be used as identifiers in any context.
     *
     * Source: [Kotlin Grammar - Hard Keywords](https://kotlinlang.org/docs/keyword-reference.html)
     */
    val HARD_KEYWORDS: Set<String> =
        setOf(
            "as",
            "break",
            "class",
            "continue",
            "do",
            "else",
            "false",
            "for",
            "fun",
            "if",
            "in",
            "interface",
            "is",
            "null",
            "object",
            "package",
            "return",
            "super",
            "this",
            "throw",
            "true",
            "try",
            "typealias",
            "val",
            "var",
            "when",
            "while",
        )

    /**
     * Soft keywords that are problematic for generated data class properties,
     * constructor parameters, or enum constants. These are valid identifiers
     * in some contexts but can cause confusion when used as property names
     * alongside visibility modifiers or in other specific syntactic positions.
     *
     * Currently this list is limited to visibility modifiers because they
     * are the most likely to collide with generated property names and
     * cause actual compilation errors. Other soft keywords (e.g. `data`,
     * `inner`, `open`) are valid identifiers in data class member positions
     * and are intentionally left unescaped.
     */
    private val PROBLEMATIC_SOFT_KEYWORDS: Set<String> =
        setOf(
            "private",
            "protected",
            "public",
            "internal",
        )

    private val ALL_KEYWORDS: Set<String> = HARD_KEYWORDS + PROBLEMATIC_SOFT_KEYWORDS

    /**
     * Returns `true` if [name] is a Kotlin keyword (hard or soft).
     */
    fun isKeyword(name: String): Boolean = name in ALL_KEYWORDS

    /**
     * Returns a safe Kotlin identifier for the given GraphQL [name].
     *
     * Hard keywords and problematic soft keywords are escaped by appending
     * `Value` (e.g. `private` -> `privateValue`).
     * Names starting with a digit are prefixed with `_`.
     * All other names are returned unchanged.
     */
    fun escapeKeyword(name: String): String {
        if (name.isEmpty()) return "_"
        if (name[0].isDigit()) return "_$name"
        if (isKeyword(name)) return "${name}Value"
        return name
    }
}
