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
 * (input object, enum, scalar, object, interface, or union) instead of treating
 * every schema name the same.
 *
 * @property inputObjects Input object definitions discovered from the schema.
 * @property enums Enum definitions discovered from the schema.
 * @property scalars Scalar definitions discovered from the schema.
 * @property objects Object type definitions discovered from the schema.
 * @property interfaces Interface type definitions discovered from the schema.
 * @property unions Union type definitions discovered from the schema.
 * @property queryTypeName The name of the root query type, or null.
 * @property mutationTypeName The name of the root mutation type, or null.
 * @property subscriptionTypeName The name of the root subscription type, or null.
 */
class SchemaIndex private constructor(
    val inputObjects: List<SchemaType.InputObject>,
    val enums: List<SchemaType.Enum>,
    val scalars: List<SchemaType.Scalar>,
    val objects: List<SchemaType.ObjectType>,
    val interfaces: List<SchemaType.InterfaceType>,
    val unions: List<SchemaType.UnionType>,
    val queryTypeName: String?,
    val mutationTypeName: String?,
    val subscriptionTypeName: String?,
    private val definitionsByName: Map<String, SchemaType>,
    private val interfaceImplementors: Map<String, Set<String>>,
    private val unionMembers: Map<String, Set<String>>,
) {
    /**
     * Returns the schema definition registered for [name], or null if the name is unknown.
     */
    fun definition(name: String): SchemaType? = definitionsByName[name]

    /**
     * Returns the set of concrete object type names that are possible
     * for the given interface or union name.
     *
     * For an interface, returns all object types that implement it.
     * For a union, returns all member type names.
     * Returns an empty set if [name] is not a known abstract type.
     */
    fun possibleTypesFor(name: String): Set<String> {
        return interfaceImplementors[name] ?: unionMembers[name] ?: emptySet()
    }

    /**
     * Returns true if [name] is a known interface or union type.
     */
    fun isAbstractType(name: String): Boolean {
        return name in interfaceImplementors || name in unionMembers
    }

    /**
     * Returns the [SchemaType.ObjectType] definition for [name], or null if not found
     * or if the definition is not an object type.
     */
    fun objectType(name: String): SchemaType.ObjectType? {
        return definitionsByName[name] as? SchemaType.ObjectType
    }

    companion object {
        /**
         * Empty schema metadata.
         */
        val EMPTY: SchemaIndex =
            from(
                types = emptyList(),
                queryTypeName = null,
                mutationTypeName = null,
                subscriptionTypeName = null,
            )

        /**
         * Builds a [SchemaIndex] from parsed schema definitions.
         *
         * @param types The parsed schema type definitions.
         * @throws IllegalArgumentException if duplicate schema names are encountered.
         */
        fun from(types: List<SchemaType>): SchemaIndex {
            return from(types, null, null, null)
        }

        /**
         * Builds a [SchemaIndex] with optional root operation type names.
         *
         * @param types The parsed schema type definitions.
         * @param queryTypeName The name of the root query type, or null.
         * @param mutationTypeName The name of the root mutation type, or null.
         * @param subscriptionTypeName The name of the root subscription type, or null.
         * @throws IllegalArgumentException if duplicate schema names are encountered.
         */
        fun from(
            types: List<SchemaType>,
            queryTypeName: String?,
            mutationTypeName: String?,
            subscriptionTypeName: String?,
        ): SchemaIndex {
            val definitionsByName = linkedMapOf<String, SchemaType>()
            val interfaceImplementors = mutableMapOf<String, MutableSet<String>>()

            types.forEach { type ->
                require(definitionsByName.put(type.name, type) == null) {
                    "Duplicate schema type '${type.name}' found while building schema index."
                }
            }

            // Collect which object types implement which interfaces
            val objectTypes = types.filterIsInstance<SchemaType.ObjectType>()
            for (obj in objectTypes) {
                for (iface in obj.interfaces) {
                    interfaceImplementors
                        .getOrPut(iface) { mutableSetOf() }
                        .add(obj.name)
                }
            }

            // Also register interfaces without any implementors
            for (iface in types.filterIsInstance<SchemaType.InterfaceType>()) {
                interfaceImplementors.getOrPut(iface.name) { mutableSetOf() }
            }

            return SchemaIndex(
                inputObjects = types.filterIsInstance<SchemaType.InputObject>(),
                enums = types.filterIsInstance<SchemaType.Enum>(),
                scalars = types.filterIsInstance<SchemaType.Scalar>(),
                objects = objectTypes,
                interfaces = types.filterIsInstance<SchemaType.InterfaceType>(),
                unions = types.filterIsInstance<SchemaType.UnionType>(),
                queryTypeName = queryTypeName,
                mutationTypeName = mutationTypeName,
                subscriptionTypeName = subscriptionTypeName,
                definitionsByName = definitionsByName,
                interfaceImplementors = interfaceImplementors,
                unionMembers =
                types.filterIsInstance<SchemaType.UnionType>().associate {
                    it.name to it.memberTypes.toSet()
                },
            )
        }
    }
}
