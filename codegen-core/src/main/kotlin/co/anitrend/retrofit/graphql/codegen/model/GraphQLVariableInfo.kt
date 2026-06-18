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

package co.anitrend.retrofit.graphql.codegen.model

/**
 * Represents a parsed GraphQL type reference from a variable or field definition.
 */
sealed class GraphQLType {
    /**
     * A named type reference, e.g. "String", "MyInput", "DateTime".
     * @param nullable Whether the type is nullable (no trailing !).
     */
    data class Named(val name: String, val nullable: Boolean = true) : GraphQLType()

    /**
     * A list type reference, e.g. "[String!]!", "[MyInput]".
     * @param of The element type.
     * @param nullable Whether the list itself is nullable.
     */
    data class List(val of: GraphQLType, val nullable: Boolean = true) : GraphQLType()
}

/**
 * Parsed information about a single GraphQL operation variable.
 *
 * @param name The variable name (e.g. "after", "first").
 * @param type The GraphQL type of the variable.
 * @param defaultValue The default value as a GraphQL literal string, or null.
 */
data class GraphQLVariableInfo(
    val name: String,
    val type: GraphQLType,
    val defaultValue: String? = null,
)
