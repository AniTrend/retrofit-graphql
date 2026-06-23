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
 * Indexed schema metadata used during code generation.
 *
 * This keeps the original [SchemaType] definitions while also providing efficient
 * lookups by schema name so generators can branch on the real schema kind
 * (input object, enum, or scalar) instead of treating every schema name the same.
 *
 * @property inputObjects Input object definitions discovered from the schema.
 * @property enums Enum definitions discovered from the schema.
 * @property scalars Scalar definitions discovered from the schema.
 */
class SchemaIndex private constructor(
    val inputObjects: List<SchemaType.InputObject>,
    val enums: List<SchemaType.Enum>,
    val scalars: List<SchemaType.Scalar>,
    private val definitionsByName: Map<String, SchemaType>,
) {
    /**
     * Returns the schema definition registered for [name], or null if the name is unknown.
     */
    fun definition(name: String): SchemaType? = definitionsByName[name]

    companion object {
        /**
         * Empty schema metadata.
         */
        val EMPTY: SchemaIndex = from(emptyList())

        /**
         * Builds a [SchemaIndex] from parsed schema definitions.
         *
         * @throws IllegalArgumentException if duplicate schema names are encountered.
         */
        fun from(types: List<SchemaType>): SchemaIndex {
            val definitionsByName = linkedMapOf<String, SchemaType>()

            types.forEach { type ->
                require(definitionsByName.put(type.name, type) == null) {
                    "Duplicate schema type '${type.name}' found while building schema index."
                }
            }

            return SchemaIndex(
                inputObjects = types.filterIsInstance<SchemaType.InputObject>(),
                enums = types.filterIsInstance<SchemaType.Enum>(),
                scalars = types.filterIsInstance<SchemaType.Scalar>(),
                definitionsByName = definitionsByName,
            )
        }
    }
}
