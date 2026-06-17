package co.anitrend.retrofit.graphql.codegen.mapping

import co.anitrend.retrofit.graphql.codegen.model.GraphQLType

/**
 * Maps GraphQL types to Kotlin type names, respecting configured scalar mappings
 * and knowledge of schema-defined input/enum types.
 */
object GraphQLTypeMapper {

    /**
     * Built-in GraphQL scalar types mapped to their Kotlin equivalents.
     * Maps non-null variants; nullable wrapping is handled by [toKotlinType].
     */
    private val BUILT_IN_SCALARS = mapOf(
        "String" to "kotlin.String",
        "Int" to "kotlin.Int",
        "Float" to "kotlin.Double",
        "Boolean" to "kotlin.Boolean",
        "ID" to "kotlin.String",
    )

    /**
     * Maps a [GraphQLType] to a Kotlin type string suitable for use in code generation.
     *
     * @param type The GraphQL type to map.
     * @param scalarMappings Custom scalar type to Kotlin type mappings (e.g. "DateTime" -> "kotlin.String").
     * @param schemaTypeNames The set of type names defined in the schema
     *   (input objects and enums). These are treated as their own type names.
     * @return A Kotlin type string (e.g. "String?", "List<MyInput>?", "Int").
     */
    fun toKotlinType(
        type: GraphQLType,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): String {
        return when (type) {
            is GraphQLType.Named -> {
                val base = resolveNamedType(type.name, scalarMappings, schemaTypeNames)
                if (type.nullable) "$base?" else base
            }
            is GraphQLType.List -> {
                val elementType = toKotlinType(type.of, scalarMappings, schemaTypeNames).let {
                    // Remove outer nullable marker since list element nullability
                    // is expressed via the nullable flag, not the ? suffix.
                    if (type.of is GraphQLType.Named && !type.of.nullable) {
                        // Non-null element inside list stays as non-null
                        it
                    } else {
                        // Element type is already nullable
                        it
                    }
                }
                val listType = "kotlin.collections.List<$elementType>"
                if (type.nullable) "$listType?" else listType
            }
        }
    }

    /**
     * Resolves a named GraphQL type to its Kotlin equivalent.
     */
    private fun resolveNamedType(
        name: String,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): String {
        // Check custom scalar mappings first
        scalarMappings[name]?.let { return it }

        // Check built-in scalars
        BUILT_IN_SCALARS[name]?.let { return it }

        // If it's a schema-defined type (input object or enum), use the type name directly
        if (name in schemaTypeNames) return name

        // Unknown scalar -- will be reported as an error by the task
        error(
            "Unknown scalar type '$name'. " +
                "Add a scalar mapping in the retrofitGraphQL {} extension, e.g.:\n" +
                "  scalars { map(\"$name\", \"kotlin.String\") }"
        )
    }
}
