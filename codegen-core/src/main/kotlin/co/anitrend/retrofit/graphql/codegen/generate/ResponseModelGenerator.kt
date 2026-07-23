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

package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.ResponseField
import co.anitrend.retrofit.graphql.codegen.model.ResponseSelectionSet
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates kotlinx-serializable response model data classes from a normalized
 * [ResponseSelectionSet] produced by [co.anitrend.retrofit.graphql.codegen.parser.ResponseSelectionParser].
 *
 * Each operation produces one top-level data class named `{OperationName}Data`
 * with nested data classes for every referenced GraphQL object type that has
 * a selection set.
 *
 * Example output:
 * ```kotlin
 * @Serializable
 * data class GetUserData(
 *     @SerialName("viewer")
 *     val viewer: User?,
 * ) {
 *     @Serializable
 *     data class User(
 *         val id: String,
 *         val login: String,
 *         val name: String?,
 *         val bio: String?,
 *     )
 * }
 * ```
 *
 * @property schemaIndex Indexed schema metadata used to resolve type definitions.
 * @property scalarMappings Custom scalar type to Kotlin type mappings
 *   (e.g. "DateTime" to "kotlin.String").
 */
class ResponseModelGenerator(
    private val schemaIndex: SchemaIndex,
    private val scalarMappings: Map<String, String> = emptyMap(),
) {
    private companion object {
        val SERIALIZABLE = ClassName("kotlinx.serialization", "Serializable")
        val SERIAL_NAME = ClassName("kotlinx.serialization", "SerialName")

        val BUILT_IN_SCALARS: Map<String, String> = mapOf(
            "String" to "kotlin.String",
            "Int" to "kotlin.Int",
            "Float" to "kotlin.Double",
            "Boolean" to "kotlin.Boolean",
            "ID" to "kotlin.String",
        )
    }

    /**
     * Generates kotlinx-serializable response model classes for a single
     * operation from its normalized response selection set.
     *
     * @param operation The GraphQL operation metadata (name, type).
     * @param selectionSet The normalized response selection from Phase 2.
     * @param packageName The target Kotlin package for generated classes.
     * @param dataClassName Optional override for the root data class name.
     *   Defaults to `{OperationName}Data`.
     * @return A list of KotlinPoet [FileSpec] ready to write.
     */
    fun generate(
        operation: GraphQLOperationInfo,
        selectionSet: ResponseSelectionSet,
        packageName: String,
        dataClassName: String = "${operation.name}Data",
    ): List<FileSpec> {
        // Collect all referenced object types and merge their selection sets
        val mergedSelections = collectAndMergeObjectTypes(selectionSet, dataClassName)

        // Collect all abstract types (interfaces/unions) and their concrete subtypes
        val abstractTypes = collectAbstractTypes(selectionSet)

        // Assign generated class names (schema type name by default, with collision detection)
        val allTypeNames = mergedSelections.keys.toList() + abstractTypes.keys.toList()
        val resolvedNames = assignClassNames(allTypeNames)

        // Generate nested data classes for object types
        val nestedObjectSpecs = mergedSelections.map { (schemaTypeName, selSet) ->
            val className = resolvedNames[schemaTypeName]!!
            generateDataClass(
                className = className,
                selectionSet = selSet,
                dataClassName = dataClassName,
                packageName = packageName,
                resolvedNames = resolvedNames,
            )
        }

        // Generate sealed interfaces for abstract types with concrete subtypes
        val sealedInterfaceSpecs = abstractTypes.map { (abstractName, concreteTypes) ->
            val ifaceName = resolvedNames[abstractName]!!
            generateSealedInterface(
                abstractName = abstractName,
                interfaceName = ifaceName,
                concreteTypes = concreteTypes,
                dataClassName = dataClassName,
                packageName = packageName,
                resolvedNames = resolvedNames,
            )
        }

        // Generate nested data classes first, since root class references them
        val nestedTypeSpecs = mergedSelections.map { (schemaTypeName, selSet) ->
            val className = resolvedNames[schemaTypeName]!!
            generateDataClass(
                className = className,
                selectionSet = selSet,
                dataClassName = dataClassName,
                packageName = packageName,
                resolvedNames = resolvedNames,
            )
        }

        // Generate root data class
        val rootTypeSpec = buildDataClass(
            className = dataClassName,
            selectionSet = selectionSet,
            dataClassName = dataClassName,
            packageName = packageName,
            resolvedNames = resolvedNames,
        ).toBuilder()
            .apply {
                nestedObjectSpecs.forEach { addType(it) }
                sealedInterfaceSpecs.forEach { addType(it) }
            }
            .build()

        return listOf(
            FileSpec.builder(packageName, dataClassName)
                .addType(rootTypeSpec)
                .build(),
        )
    }

    // --- Private helpers ---

    /**
     * Walks the selection tree and collects all object types with their
     * merged selection sets, keyed by response-path identity instead of
     * schema type name. This ensures each unique response path produces
     * its own model class rather than merging different selections of
     * the same schema type.
     */
    private fun collectAndMergeObjectTypes(
        selectionSet: ResponseSelectionSet,
        dataClassName: String,
    ): LinkedHashMap<String, ResponseSelectionSet> {
        val result = linkedMapOf<String, ResponseSelectionSet>()

        fun walk(selSet: ResponseSelectionSet) {
            for (field in selSet.fields) {
                val typeName = resolveNamedType(field.outputType)
                val def = schemaIndex.definition(typeName)

                if (def is SchemaType.ObjectType && field.selectionSet != null) {
                    val key = field.selectionSet.responseIdentity.ifBlank {
                        dataClassName
                    }
                    val existing = result[key]
                    if (existing != null) {
                        result[key] = mergeSelectionSets(
                            existing,
                            field.selectionSet,
                        )
                    } else {
                        result[key] = field.selectionSet
                    }
                    walk(field.selectionSet)
                }

                // Skip interfaces and unions for now (handled by collectAbstractTypes)
            }
        }

        walk(selectionSet)
        return result
    }

    /**
     * Walks the selection tree and collects all abstract types (interfaces
     * and unions) that have concrete subtype selections via inline fragments.
     * Returns a map of abstract type name -> list of (concreteTypeName, selectionSet).
     */
    private fun collectAbstractTypes(
        selectionSet: ResponseSelectionSet,
    ): LinkedHashMap<String, List<Pair<String, ResponseSelectionSet>>> {
        val result = linkedMapOf<String, MutableList<Pair<String, ResponseSelectionSet>>>()

        fun walk(selSet: ResponseSelectionSet) {
            for (field in selSet.fields) {
                val typeName = resolveNamedType(field.outputType)
                val def = schemaIndex.definition(typeName)

                if (def is SchemaType.InterfaceType || def is SchemaType.UnionType) {
                    // Use response-path identity as the key so the same
                    // abstract type appearing at different response paths
                    // (e.g. aliased siblings) each gets its own sealed
                    // hierarchy. Mirrors the fix in collectAndMergeObjectTypes().
                    val identity =
                        field.selectionSet?.responseIdentity
                            ?.ifBlank { field.responseName }
                            ?: typeName
                    val concreteEntries = result.getOrPut(identity) { mutableListOf() }

                    // The parser merges inline fragment fields into the
                    // interface's selection set. Each possible concrete type
                    // gets the full merged selection set as its own.
                    if (field.selectionSet != null && field.possibleTypes.isNotEmpty()) {
                        for (concreteType in field.possibleTypes) {
                            concreteEntries.add(
                                concreteType to field.selectionSet,
                            )
                        }
                    }
                }

                if (field.selectionSet != null) {
                    walk(field.selectionSet)
                }
            }
        }

        walk(selectionSet)
        return LinkedHashMap(result)
    }

    /**
     * Generates a sealed interface TypeSpec for an abstract type with concrete
     * data class subtypes, using `@JsonClassDiscriminator` for automatic
     * `__typename`-based deserialization.
     */
    private fun generateSealedInterface(
        abstractName: String,
        interfaceName: String,
        concreteTypes: List<Pair<String, ResponseSelectionSet>>,
        dataClassName: String,
        packageName: String,
        resolvedNames: Map<String, String>,
    ): TypeSpec {
        val interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
            .addModifiers(KModifier.PUBLIC, KModifier.SEALED)
            .addAnnotation(SERIALIZABLE)
            .addAnnotation(
                AnnotationSpec.builder(
                    ClassName("kotlin", "OptIn"),
                )
                    .addMember(
                        "%T::class",
                        ClassName(
                            "kotlinx.serialization",
                            "ExperimentalSerializationApi",
                        ),
                    )
                    .build(),
            )
            .addAnnotation(
                AnnotationSpec.builder(
                    ClassName("kotlinx.serialization.json", "JsonClassDiscriminator"),
                )
                    .addMember("%S", "__typename")
                    .build(),
            )
            .addKdoc(
                "Sealed interface for the GraphQL abstract type `%L`. " +
                    "Concrete subtypes are discriminated by `__typename` via " +
                    "`@JsonClassDiscriminator` on this interface.",
                abstractName,
            )

        for ((concreteTypeName, selSet) in concreteTypes) {
            val className = resolvedNames[concreteTypeName] ?: concreteTypeName

            // Filter fields to only those applicable to this concrete type.
            // Exclude __typename: the discriminator is handled by
            // @JsonClassDiscriminator on the sealed interface, not by a
            // data class property. Including it causes a collision.
            val sortedFields = selSet.fields.sortedBy { it.responseName }
            val applicableFields = sortedFields.filter { field ->
                field.schemaName != "__typename" &&
                    (
                        field.applicableTypes.isEmpty() ||
                            concreteTypeName in field.applicableTypes
                        )
            }
            val properties =
                applicableFields.map { field ->
                    toPropertySpec(
                        field = field,
                        dataClassName = dataClassName,
                        packageName = packageName,
                        resolvedNames = resolvedNames,
                    )
                }
            val constructorParams =
                applicableFields.zip(properties).map { (field, prop) ->
                    val paramBuilder =
                        com.squareup.kotlinpoet.ParameterSpec
                            .builder(prop.name, prop.type)
                    if (field.condition.isConditional && prop.type.isNullable) {
                        paramBuilder.defaultValue("null")
                    }
                    paramBuilder.build()
                }

            val subtypeSpec = TypeSpec.classBuilder(className)
                .addModifiers(KModifier.PUBLIC, KModifier.DATA)
                .addAnnotation(SERIALIZABLE)
                .addAnnotation(
                    AnnotationSpec.builder(SERIAL_NAME)
                        .addMember("%S", concreteTypeName)
                        .build(),
                )
                .addSuperinterface(
                    ClassName(packageName, dataClassName, interfaceName),
                )
                .primaryConstructor(
                    com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                        .addParameters(constructorParams)
                        .build(),
                )
                .apply {
                    properties.forEach { prop ->
                        val builder = prop.toBuilder()
                        builder.initializer(prop.name)
                        addProperty(builder.build())
                    }
                }
                .build()
            interfaceBuilder.addType(subtypeSpec)
        }

        return interfaceBuilder.build()
    }

    /**
     * Merges two [ResponseSelectionSet]s with the same [ResponseSelectionSet.parentType],
     * combining their fields. Duplicate fields by [ResponseField.responseName] are
     * resolved by taking the one from [other] (last-wins).
     */
    private fun mergeSelectionSets(
        base: ResponseSelectionSet,
        other: ResponseSelectionSet,
    ): ResponseSelectionSet {
        require(base.parentType == other.parentType) {
            "Cannot merge selection sets with different parent types: " +
                "'${base.parentType}' and '${other.parentType}'"
        }

        val mergedFields = linkedMapOf<String, ResponseField>()
        for (field in (base.fields + other.fields)) {
            mergedFields[field.responseName] = field
        }

        return ResponseSelectionSet(
            parentType = base.parentType,
            responseIdentity = base.responseIdentity,
            fields = mergedFields.values.sortedBy { it.responseName },
        )
    }

    /**
     * Assigns generated class names. Defaults to the schema type name.
     * Handles naming collisions from the [collision-safe spec](#) by prepending
     * the parent context.
     *
     * In practice, GraphQL schema type names are unique so collisions are rare
     * in Phase 3, but this check guards against future phases where abstract
     * type resolution may produce conflicting names.
     */
    private fun assignClassNames(schemaTypeNames: List<String>): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val seen = mutableMapOf<String, Int>() // generatedName -> count

        for (name in schemaTypeNames) {
            val count = seen.getOrDefault(name, 0)
            if (count == 0) {
                result[name] = name
                seen[name] = 1
            } else {
                // Collision: append numeric suffix
                val uniqueName = "${name}${count + 1}"
                result[name] = uniqueName
                seen[name] = count + 1
            }
        }

        return result
    }

    /**
     * Extracts the innermost named type from a [GraphQLType], stripping
     * list and nullability wrappers.
     */
    private fun resolveNamedType(type: GraphQLType): String {
        return when (type) {
            is GraphQLType.Named -> type.name
            is GraphQLType.List -> resolveNamedType(type.of)
        }
    }

    /**
     * Generates a [TypeSpec] for a data class built from a [ResponseSelectionSet].
     */
    private fun buildDataClass(
        className: String,
        selectionSet: ResponseSelectionSet,
        dataClassName: String,
        packageName: String,
        resolvedNames: Map<String, String>,
    ): TypeSpec {
        val sortedFields = selectionSet.fields.sortedBy { it.responseName }

        // Build properties first so we can extract types for constructor params
        val properties =
            sortedFields.map { field ->
                toPropertySpec(
                    field = field,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    resolvedNames = resolvedNames,
                )
            }

        // Build constructor parameters from the properties, adding null
        // defaults on the parameter (not the property body) for conditional
        // fields so they become `val name: Type? = null` in the primary
        // constructor instead of an invalid body initializer.
        val constructorParams =
            sortedFields.zip(properties).map { (field, prop) ->
                val paramBuilder =
                    com.squareup.kotlinpoet.ParameterSpec
                        .builder(prop.name, prop.type)
                if (field.condition.isConditional && prop.type.isNullable) {
                    paramBuilder.defaultValue("null")
                }
                paramBuilder.build()
            }

        return TypeSpec.classBuilder(className)
            .addModifiers(KModifier.PUBLIC, KModifier.DATA)
            .addAnnotation(SERIALIZABLE)
            .primaryConstructor(
                com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                    .addParameters(constructorParams)
                    .build(),
            )
            .apply {
                properties.forEach { prop ->
                    val builder = prop.toBuilder()
                    builder.initializer(prop.name)
                    addProperty(builder.build())
                }
            }
            .build()
    }

    /**
     * Convenience overload that generates and returns the TypeSpec directly,
     * used by the recursive collector.
     */
    private fun generateDataClass(
        className: String,
        selectionSet: ResponseSelectionSet,
        dataClassName: String,
        packageName: String,
        resolvedNames: Map<String, String>,
    ): TypeSpec {
        return buildDataClass(className, selectionSet, dataClassName, packageName, resolvedNames)
    }

    /**
     * Generates a [PropertySpec] from a [ResponseField], resolving the Kotlin
     * type and optionally adding a [@SerialName][kotlinx.serialization.SerialName]
     * annotation when the response name differs from the schema field name.
     */
    private fun toPropertySpec(
        field: ResponseField,
        dataClassName: String,
        packageName: String,
        resolvedNames: Map<String, String>,
    ): PropertySpec {
        var kotlinType = resolveTypeName(
            graphQLType = field.outputType,
            dataClassName = dataClassName,
            packageName = packageName,
            resolvedNames = resolvedNames,
            selectionIdentity = field.selectionSet?.responseIdentity,
        )

        // Conditional fields (@include/@skip) may be absent from the response
        // even when the schema type is non-null. Make them nullable with a
        // null default so the JSON decoder doesn't fail on omission.
        val isConditional = field.condition.isConditional
        if (isConditional && !kotlinType.isNullable) {
            kotlinType = kotlinType.copy(nullable = true)
        }

        val spec = PropertySpec.builder(field.responseName, kotlinType)
            .addModifiers(KModifier.PUBLIC)

        if (field.responseName != field.schemaName) {
            spec.addAnnotation(
                AnnotationSpec.builder(SERIAL_NAME)
                    .addMember("%S", field.responseName)
                    .build(),
            )
        }

        return spec.build()
    }

    /**
     * Resolves a [GraphQLType] to a KotlinPoet [TypeName], using schema
     * definition lookups to determine whether a named type corresponds to
     * a generated response class, a built-in scalar, a custom scalar mapping,
     * or an unsupported abstract type.
     *
     * @param selectionIdentity The response-path identity of the field's
     *   selection set, used to look up the correct per-path generated class.
     *   Null for leaf fields that have no nested selection set.
     */
    private fun resolveTypeName(
        graphQLType: GraphQLType,
        dataClassName: String,
        packageName: String,
        resolvedNames: Map<String, String>,
        selectionIdentity: String? = null,
    ): TypeName {
        return when (graphQLType) {
            is GraphQLType.Named -> {
                val base = resolveNamedTypeName(
                    name = graphQLType.name,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    resolvedNames = resolvedNames,
                    selectionIdentity = selectionIdentity,
                )
                if (graphQLType.nullable) base.copy(nullable = true) else base
            }
            is GraphQLType.List -> {
                val elementType = resolveTypeName(
                    graphQLType = graphQLType.of,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    resolvedNames = resolvedNames,
                    selectionIdentity = selectionIdentity,
                )
                val listType = ClassName("kotlin.collections", "List")
                    .parameterizedBy(elementType)
                if (graphQLType.nullable) listType.copy(nullable = true) else listType
            }
        }
    }

    /**
     * Resolves a single named GraphQL type to a KotlinPoet [TypeName].
     *
     * Priority:
     * 1. Custom scalar mappings
     * 2. Built-in GraphQL scalars (String, Int, Float, Boolean, ID)
     * 3. Generated response classes, looked up by response-path identity
     *    first, then by schema type name as fallback
     * 4. Schema-defined types (interfaces, unions) — produces a TODO stub
     * 5. Unknown types — throws an error suggesting a scalar mapping
     *
     * @param selectionIdentity The response-path identity of the field's
     *   own selection set, used as the primary lookup key for generated
     *   classes. Falls back to [name] (schema type name) when null/blank
     *   or not found.
     */
    private fun resolveNamedTypeName(
        name: String,
        dataClassName: String,
        packageName: String,
        resolvedNames: Map<String, String>,
        selectionIdentity: String? = null,
    ): TypeName {
        // 1. Custom scalar mappings
        scalarMappings[name]?.let { return parseFqcnToTypeName(it) }

        // 2. Built-in scalars
        BUILT_IN_SCALARS[name]?.let { return parseFqcnToTypeName(it) }

        // 3. Generated response class — look up by response-path identity
        //    first, falling back to schema type name for abstract types
        //    and backwards compatibility.
        val lookupKey =
            if (selectionIdentity.isNullOrBlank()) name else selectionIdentity
        resolvedNames[lookupKey]?.let { generatedName ->
            return ClassName(packageName, dataClassName, generatedName)
        }
        // Fallback: schema type name (needed when selectionIdentity differs
        // from the key used in resolvedNames, e.g. for abstract types)
        if (lookupKey != name) {
            resolvedNames[name]?.let { generatedName ->
                return ClassName(packageName, dataClassName, generatedName)
            }
        }

        // 4. Schema-defined type
        val def = schemaIndex.definition(name)
        when (def) {
            is SchemaType.ObjectType -> {
                // Object type that was not collected from the selection tree.
                // This means the field is selected without a sub-selection (rare
                // but possible with fragments that weren't inlined).
                // Fall back to the schema type name as a direct class reference.
                error(
                    "Object type '$name' is referenced but has no selection set " +
                        "in the response. This may indicate a missing fragment expansion.",
                )
            }
            is SchemaType.InterfaceType,
            is SchemaType.UnionType,
            -> {
                // Abstract types map to generated sealed interfaces nested
                // inside the root Data class.
                resolvedNames[name]?.let { generatedName ->
                    return ClassName(packageName, dataClassName, generatedName)
                }
                error(
                    "Abstract type '$name' was not collected from the selection tree. " +
                        "This indicates a processing error.",
                )
            }
            is SchemaType.InputObject,
            is SchemaType.Enum,
            -> {
                // Input/enum types in response context — use mapped type
                return ClassName(packageName, name)
            }
            is SchemaType.Scalar -> {
                error(
                    "Unknown scalar type '$name'. " +
                        "Add a scalar mapping in the retrofitGraphQL {} extension, e.g.:\n" +
                        "  scalars { map(\"$name\", \"kotlin.String\") }",
                )
            }
            null -> {
                error(
                    "Unknown type '$name' is not defined in the schema. " +
                        "Add a scalar mapping in the retrofitGraphQL {} extension, e.g.:\n" +
                        "  scalars { map(\"$name\", \"kotlin.String\") }",
                )
            }
        }
    }

    /**
     * Parses a fully-qualified Kotlin type name string into a [ClassName].
     *
     * Handles nested classes like "okhttp3.MultipartBody.Part" by heuristically
     * splitting on the first uppercase-starting segment as the class boundary.
     */
    private fun parseFqcnToTypeName(fqcn: String): ClassName {
        val parts = fqcn.split(".")
        val firstClassIndex = parts.indexOfLast { it.first().isLowerCase() } + 1
            .coerceAtLeast(1)
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
