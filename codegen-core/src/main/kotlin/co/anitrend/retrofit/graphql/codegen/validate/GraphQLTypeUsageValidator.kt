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

package co.anitrend.retrofit.graphql.codegen.validate

import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType

/**
 * Validates that generated variable/input/request-helper types only reference supported scalars.
 */
object GraphQLTypeUsageValidator {
    private val BUILT_IN_SCALARS = setOf("String", "Int", "Float", "Boolean", "ID")

    /**
     * Validates operation variable graphs and all generated input objects.
     */
    fun validate(
        operations: List<GraphQLOperationInfo>,
        schemaIndex: SchemaIndex,
        scalarMappings: Map<String, String>,
    ) {
        operations.forEach { operation ->
            operation.variables.forEach { variable ->
                validateType(
                    type = variable.type,
                    schemaIndex = schemaIndex,
                    scalarMappings = scalarMappings,
                    path = "operation '${operation.name}' variable '${variable.name}'",
                    visitedInputObjects = linkedSetOf(),
                )
            }
        }

        schemaIndex.inputObjects.forEach { inputObject ->
            inputObject.fields.forEach { field ->
                validateType(
                    type = field.type,
                    schemaIndex = schemaIndex,
                    scalarMappings = scalarMappings,
                    path = "input object '${inputObject.name}.${field.name}'",
                    visitedInputObjects = linkedSetOf(inputObject.name),
                )
            }
        }
    }

    private fun validateType(
        type: GraphQLType,
        schemaIndex: SchemaIndex,
        scalarMappings: Map<String, String>,
        path: String,
        visitedInputObjects: MutableSet<String>,
    ) {
        when (type) {
            is GraphQLType.List -> validateType(
                type = type.of,
                schemaIndex = schemaIndex,
                scalarMappings = scalarMappings,
                path = "$path[]",
                visitedInputObjects = visitedInputObjects,
            )
            is GraphQLType.Named -> validateNamedType(
                name = type.name,
                schemaIndex = schemaIndex,
                scalarMappings = scalarMappings,
                path = path,
                visitedInputObjects = visitedInputObjects,
            )
        }
    }

    private fun validateNamedType(
        name: String,
        schemaIndex: SchemaIndex,
        scalarMappings: Map<String, String>,
        path: String,
        visitedInputObjects: MutableSet<String>,
    ) {
        if (name in BUILT_IN_SCALARS || name in scalarMappings) return

        when (val definition = schemaIndex.definition(name)) {
            is SchemaType.Enum -> return
            is SchemaType.Scalar -> {
                throw IllegalArgumentException(
                    "Unknown scalar type '$name' at $path. " +
                        "Add a scalar mapping in the retrofitGraphQL {} extension, e.g.:\n" +
                        "  scalars { map(\"$name\", \"kotlin.String\") }",
                )
            }
            is SchemaType.InputObject -> {
                if (!visitedInputObjects.add(definition.name)) return

                definition.fields.forEach { field ->
                    validateType(
                        type = field.type,
                        schemaIndex = schemaIndex,
                        scalarMappings = scalarMappings,
                        path = "$path.${field.name}",
                        visitedInputObjects = visitedInputObjects,
                    )
                }

                visitedInputObjects.remove(definition.name)
            }
            null -> {
                throw IllegalArgumentException(
                    "Unknown type '$name' at $path. " +
                        "Add a scalar mapping in the retrofitGraphQL {} extension, e.g.:\n" +
                        "  scalars { map(\"$name\", \"kotlin.String\") }",
                )
            }
        }
    }
}
