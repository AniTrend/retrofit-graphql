package co.anitrend.retrofit.graphql.codegen.mapping

import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

/**
 * Maps GraphQL types to KotlinPoet TypeName instances, respecting configured scalar mappings
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
     * Maps a [GraphQLType] to a KotlinPoet [TypeName] suitable for use in code generation.
     *
     * @param type The GraphQL type to map.
     * @param scalarMappings Custom scalar type to Kotlin type mappings (e.g. "DateTime" -> "kotlin.String").
     * @param schemaTypeNames The set of type names defined in the schema
     *   (input objects and enums). These are treated as their own type names.
     * @return A KotlinPoet [TypeName] (e.g. String?, List<MyInput>?, Int).
     */
    fun toKotlinType(
        type: GraphQLType,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): TypeName {
        return when (type) {
            is GraphQLType.Named -> {
                val base = resolveNamedType(type.name, scalarMappings, schemaTypeNames)
                if (type.nullable) base.copy(nullable = true) else base
            }
            is GraphQLType.List -> {
                val elementType = toKotlinType(type.of, scalarMappings, schemaTypeNames)
                val listType = ClassName("kotlin.collections", "List")
                    .parameterizedBy(elementType)
                if (type.nullable) listType.copy(nullable = true) else listType
            }
        }
    }

    /**
     * Resolves a named GraphQL type to its KotlinPoet TypeName equivalent.
     */
    private fun resolveNamedType(
        name: String,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): TypeName {
        // Check custom scalar mappings first
        scalarMappings[name]?.let { return parseFqcnToTypeName(it) }

        // Check built-in scalars
        BUILT_IN_SCALARS[name]?.let { return parseFqcnToTypeName(it) }

        // If it's a schema-defined type (input object or enum), use the type name directly
        if (name in schemaTypeNames) return ClassName("", name)

        // Unknown scalar -- will be reported as an error by the task
        error(
            "Unknown scalar type '$name'. " +
                "Add a scalar mapping in the retrofitGraphQL {} extension, e.g.:\n" +
                "  scalars { map(\"$name\", \"kotlin.String\") }"
        )
    }

    /**
     * Parses a fully-qualified Kotlin type name string into a [ClassName].
     *
     * Handles nested classes like "okhttp3.MultipartBody.Part" by heuristically
     * splitting on the first uppercase-starting segment as the class boundary.
     * All lowercase-starting segments before it form the package name.
     */
    private fun parseFqcnToTypeName(fqcn: String): ClassName {
        val parts = fqcn.split(".")
        // Find the split point between package (lowercase) and class hierarchy (uppercase)
        val firstClassIndex = parts.indexOfLast { it.first().isLowerCase() } + 1
        val packageName = parts.subList(0, firstClassIndex).joinToString(".")
        val classNames = parts.subList(firstClassIndex, parts.size)

        return when (classNames.size) {
            0 -> error("Invalid fully-qualified class name: $fqcn")
            1 -> ClassName(packageName, classNames[0])
            else -> {
                var className = ClassName(packageName, classNames[0])
                for (i in 1 until classNames.size) {
                    className = className.nestedClass(classNames[i])
                }
                className
            }
        }
    }
}
