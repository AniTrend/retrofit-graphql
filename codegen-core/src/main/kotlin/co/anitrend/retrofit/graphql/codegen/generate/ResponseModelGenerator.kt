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

        // Collect all known concrete type names (used to derive scope prefix
        // from dotted identity keys for nested type resolution inside sealed
        // interface subtypes)
        val allConcreteTypeNames = abstractTypes.values
            .flatten()
            .map { (name, _) -> name }
            .toSet()

        // Assign generated class names from dotted response-path identities
        val allTypeNames = mergedSelections.keys.toList() + abstractTypes.keys.toList()
        val resolvedNames = assignClassNames(allTypeNames)

        // Generate nested data classes for object types, threading scope
        // prefix where the dotted identity starts with a concrete type name
        val nestedObjectSpecs = mergedSelections.map { (dottedIdentity, selSet) ->
            val className = resolvedNames[dottedIdentity]!!
            val scopePrefix = deriveScopePrefix(dottedIdentity, allConcreteTypeNames)
            generateDataClass(
                className = className,
                selectionSet = selSet,
                dataClassName = dataClassName,
                packageName = packageName,
                resolvedNames = resolvedNames,
                scopePrefix = scopePrefix,
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
                    // Check if nested fields within this selection set
                    // have different runtimeBranches (meaning they come
                    // from different fragment branches of an abstract
                    // parent). If so, split into per-scope entries to
                    // avoid merging unrelated fields from mutually
                    // exclusive subtype branches into one class.
                    val nestedGroups =
                        field.selectionSet.fields.groupBy {
                            it.runtimeBranches
                        }

                    // Also collect alternatives for projection
                    val alternatives =
                        field.selectionSet.fields.flatMap { f ->
                            f.alternatives
                        }

                    // Also split when the field itself has multiple
                    // runtimeBranches (merged from multiple fragment
                    // branches) even if its immediate children are
                    // not yet split. This ensures parent entries exist
                    // for deeply nested scope resolution.
                    val needsSplit = nestedGroups.size > 1 ||
                        field.runtimeBranches.size > 1

                    if (!needsSplit) {
                        // All fields share the same scope — normal case
                        val key =
                            field.selectionSet.responseIdentity.ifBlank {
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
                    } else {
                        val baseIdentity =
                            field.selectionSet.responseIdentity
                                .ifBlank { dataClassName }
                        val allCandidates =
                            field.selectionSet.fields + alternatives

                        // Determine the maximum abstract depth from all
                        // branches. If all branches are at depth 1
                        // (single abstract level), iterate by individual
                        // concrete types so that fields belonging to
                        // multiple types (like a common `id`) appear in
                        // every type's projection.
                        val allBranches =
                            allCandidates.flatMap { it.runtimeBranches }
                        val maxDepth =
                            allBranches.maxOfOrNull {
                                it.abstractPath.size
                            } ?: 0

                        // Group the child fields by their branch set
                        val fieldGroups =
                            nestedGroups.filterKeys { it.isNotEmpty() }

                        if (maxDepth <= 1) {
                            // Single abstract level: iterate by
                            // individual concrete types from the schema
                            // parent definition, collecting fields that
                            // include that type in their branch set.
                            // This naturally includes common fields
                            // (which belong to multiple types).
                            val enclosingDef =
                                schemaIndex.definition(
                                    selSet.parentType,
                                )
                            val schemaTypes =
                                when (enclosingDef) {
                                    is SchemaType.InterfaceType ->
                                        enclosingDef.possibleTypes.toSet()
                                    is SchemaType.UnionType ->
                                        enclosingDef.memberTypes.toSet()
                                    else -> emptySet()
                                }

                            val typesToProject =
                                if (schemaTypes.isNotEmpty()) {
                                    schemaTypes
                                } else {
                                    allBranches
                                        .map { it.concreteType }
                                        .toSet()
                                }

                            for (concreteType in typesToProject) {
                                val allSelectedTypes =
                                    allCandidates
                                        .flatMap { f ->
                                            f.runtimeBranches.map {
                                                it.concreteType
                                            }
                                        }.toSet()
                                val projectedFields =
                                    allCandidates.filter { f ->
                                        val branchTypes =
                                            f.runtimeBranches
                                                .map {
                                                    it.concreteType
                                                }.toSet()
                                        branchTypes.isEmpty() ||
                                            concreteType in branchTypes ||
                                            (
                                                branchTypes
                                                    .containsAll(
                                                        allSelectedTypes,
                                                    ) &&
                                                    allSelectedTypes
                                                        .isNotEmpty()
                                                )
                                    }
                                        .distinctBy { it.responseName }

                                if (projectedFields.isEmpty()) continue

                                val scopedIdentity =
                                    "$concreteType.$baseIdentity"
                                val scopedSelSet =
                                    field.selectionSet.copy(
                                        responseIdentity =
                                        scopedIdentity,
                                        fields = projectedFields
                                            .sortedBy {
                                                it.responseName
                                            },
                                    )
                                val existing = result[scopedIdentity]
                                result[scopedIdentity] =
                                    if (existing != null) {
                                        mergeSelectionSets(
                                            existing,
                                            scopedSelSet,
                                        )
                                    } else {
                                        scopedSelSet
                                    }
                            }
                        } else {
                            // Multi-level abstract hierarchy:
                            // iterate over distinct branch chains.
                            // Each chain represents a concrete type
                            // path (e.g. OuterA → InnerX).
                            for (
                            (branchChain, scopedFields) in fieldGroups
                            ) {
                                // Include fields with empty branches
                                // (common to all types) in each scope
                                val commonFields =
                                    allCandidates.filter { f ->
                                        f.runtimeBranches.isEmpty()
                                    }
                                val allScopedFields =
                                    (commonFields + scopedFields)
                                        .distinctBy { it.responseName }

                                if (allScopedFields.isEmpty()) continue

                                val chainTypes =
                                    branchChain.map {
                                        it.concreteType
                                    }
                                val scopedIdentity =
                                    "${
                                        chainTypes.joinToString(".")
                                    }.$baseIdentity"
                                val scopedSelSet =
                                    field.selectionSet.copy(
                                        responseIdentity =
                                        scopedIdentity,
                                        fields = allScopedFields
                                            .sortedBy {
                                                it.responseName
                                            },
                                    )
                                val existing = result[scopedIdentity]
                                result[scopedIdentity] =
                                    if (existing != null) {
                                        mergeSelectionSets(
                                            existing,
                                            scopedSelSet,
                                        )
                                    } else {
                                        scopedSelSet
                                    }
                            }

                            // Item 1: also create entries for
                            // missing types from the schema
                            val enclosingDef =
                                schemaIndex.definition(
                                    selSet.parentType,
                                )
                            val allPossibleTypes =
                                when (enclosingDef) {
                                    is SchemaType.InterfaceType ->
                                        enclosingDef.possibleTypes
                                            .toSet()
                                    is SchemaType.UnionType ->
                                        enclosingDef.memberTypes
                                            .toSet()
                                    else -> emptySet()
                                }

                            if (allPossibleTypes.isNotEmpty()) {
                                val projectedTypes =
                                    fieldGroups.keys
                                        .flatMap { chain ->
                                            chain.map {
                                                it.concreteType
                                            }
                                        }.toSet()
                                val missingTypes =
                                    allPossibleTypes -
                                        projectedTypes

                                // Compute fields that are common
                                // across ALL selected types — these
                                // should appear even for types that
                                // have no type-specific selections.
                                val allSelectedTypes =
                                    allCandidates
                                        .flatMap { f ->
                                            f.runtimeBranches.map {
                                                it.concreteType
                                            }
                                        }.toSet()
                                val commonToAllSelected =
                                    allCandidates.filter { f ->
                                        f.runtimeBranches
                                            .isEmpty() ||
                                            f.runtimeBranches
                                                .map {
                                                    it.concreteType
                                                }.toSet()
                                                .containsAll(
                                                    allSelectedTypes,
                                                )
                                    }
                                if (
                                    commonToAllSelected.isEmpty()
                                ) {
                                    continue
                                }
                                for (missingType in missingTypes) {
                                    val scopedIdentity =
                                        "$missingType.$baseIdentity"
                                    if (
                                        result.containsKey(
                                            scopedIdentity,
                                        )
                                    ) {
                                        continue
                                    }
                                    val scopedSelSet =
                                        field.selectionSet.copy(
                                            responseIdentity =
                                            scopedIdentity,
                                            fields =
                                            commonToAllSelected
                                                .sortedBy {
                                                    it.responseName
                                                },
                                        )
                                    result[scopedIdentity] =
                                        scopedSelSet
                                }
                            }
                        }
                    }
                    walk(field.selectionSet)
                } else if (field.selectionSet != null) {
                    // Walk into abstract (interface/union) selection sets
                    // to find nested concrete objects (e.g.
                    // AiringNotification.media.title inside a notification
                    // union inline fragment).
                    walk(field.selectionSet)
                }
            }
        }

        walk(selectionSet)
        return result
    }

    /**
     * Derives the scope prefix from a dotted identity key for nested
     * type resolution inside sealed interface subtypes.
     *
     * When the first segment of a dotted identity matches a known
     * concrete type name (e.g. "Success" in "Success.result.detail"),
     * that segment is the scope prefix that should be threaded through
     * child property generation. Otherwise returns null.
     */
    private fun deriveScopePrefix(
        dottedIdentity: String,
        concreteTypeNames: Set<String>,
    ): String? {
        if (!dottedIdentity.contains('.')) return null
        val firstSegment = dottedIdentity.substringBefore('.')
        return if (firstSegment in concreteTypeNames) firstSegment else null
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
                // Only remove the unaliased discriminator __typename.
                // Aliased fields like `kind: __typename` should remain
                // as regular properties on the subtype.
                !(field.schemaName == "__typename" && field.responseName == "__typename") &&
                    (
                        field.computeApplicableTypes().isEmpty() ||
                            concreteTypeName in field.computeApplicableTypes()
                        )
            }
            val properties =
                applicableFields.map { field ->
                    toPropertySpec(
                        field = field,
                        dataClassName = dataClassName,
                        packageName = packageName,
                        resolvedNames = resolvedNames,
                        scopePrefix = concreteTypeName,
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

            val subtypeSpec = if (applicableFields.isEmpty()) {
                // Unselected union/interfaces members have no fields at all.
                // Data classes require at least one primary-constructor parameter,
                // so emit a plain serializable class instead.
                TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.PUBLIC)
                    .addAnnotation(SERIALIZABLE)
                    .addAnnotation(
                        AnnotationSpec.builder(SERIAL_NAME)
                            .addMember("%S", concreteTypeName)
                            .build(),
                    )
                    .addSuperinterface(
                        ClassName(packageName, dataClassName, interfaceName),
                    )
                    .build()
            } else {
                TypeSpec.classBuilder(className)
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
            }
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
     * Assigns generated class names from dotted response-path identities.
     * Converts dot-separated identities (e.g. "updateBio.user") to PascalCase
     * class names (e.g. "UpdateBioUser"). Handles naming collisions by appending
     * numeric suffixes.
     *
     * Dot-separated identities cannot collide at the map-key level because
     * '.' is not valid in GraphQL field names, so "foo.bar" (nested) and
     * "fooBar" (flat) are always distinct keys. Class name collisions from
     * different identities (e.g. both map to "FooBar") are resolved via
     * numeric suffixes.
     */
    private fun assignClassNames(dottedIdentities: List<String>): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val seen = mutableMapOf<String, Int>() // pascalCaseName -> count

        for (identity in dottedIdentities) {
            val className = dottedToPascalCase(identity)
            val count = seen.getOrDefault(className, 0)
            if (count == 0) {
                result[identity] = className
                seen[className] = 1
            } else {
                // Collision: append numeric suffix
                val uniqueName = "${className}${count + 1}"
                result[identity] = uniqueName
                seen[className] = count + 1
            }
        }

        return result
    }

    /**
     * Converts a dot-separated response-path identity to PascalCase.
     * Example: "updateBio.user" → "UpdateBioUser"
     */
    private fun dottedToPascalCase(identity: String): String =
        identity.split(".").joinToString("") {
            it.replaceFirstChar { c -> c.uppercase() }
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
        scopePrefix: String? = null,
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
                    scopePrefix = scopePrefix,
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
        scopePrefix: String? = null,
    ): TypeSpec {
        return buildDataClass(
            className,
            selectionSet,
            dataClassName,
            packageName,
            resolvedNames,
            scopePrefix,
        )
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
        scopePrefix: String? = null,
    ): PropertySpec {
        var kotlinType = resolveTypeName(
            graphQLType = field.outputType,
            dataClassName = dataClassName,
            packageName = packageName,
            resolvedNames = resolvedNames,
            selectionIdentity = field.selectionSet?.responseIdentity,
            scopePrefix = scopePrefix,
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
        scopePrefix: String? = null,
    ): TypeName {
        return when (graphQLType) {
            is GraphQLType.Named -> {
                val base = resolveNamedTypeName(
                    name = graphQLType.name,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    resolvedNames = resolvedNames,
                    selectionIdentity = selectionIdentity,
                    scopePrefix = scopePrefix,
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
                    scopePrefix = scopePrefix,
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
        scopePrefix: String? = null,
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
        // Per-scope fallback: when a sealed interface subtype has a nested
        // ObjectType field whose identity was split by the collector
        // (e.g. "result.detail" → "Success.result.detail"), the identity
        // on the field still points to the merged key. Try prefixing with
        // the concrete type name that owns this field.
        //
        // For multi-level scoped identifiers (e.g.
        // "OuterA.InnerX.outer.inner.detail"), try progressively longer
        // prefix combinations derived from the scopePrefix segments.
        if (!scopePrefix.isNullOrBlank() && !lookupKey.isNullOrBlank()) {
            // Try exact prefix match first
            val scopedKey = "$scopePrefix.$lookupKey"
            resolvedNames[scopedKey]?.let { generatedName ->
                return ClassName(packageName, dataClassName, generatedName)
            }

            // For multi-level chains, try partial prefixes.
            // The scopePrefix may be "OuterA" but the actual key
            // could be "OuterA.InnerX.outer.inner.detail". Search for
            // any key that:
            //   a) ends with ".{lookupKey}"
            //   b) contains scopePrefix as a prefix component
            val prefixParts = scopePrefix.split(".")
            val candidates =
                resolvedNames.keys.filter { key ->
                    key.endsWith(".$lookupKey") &&
                        prefixParts.all { part ->
                            key.split(".")
                                .contains(part)
                        }
                }
            if (candidates.size == 1) {
                resolvedNames[candidates.first()]?.let {
                        generatedName ->
                    return ClassName(
                        packageName,
                        dataClassName,
                        generatedName,
                    )
                }
            } else if (candidates.isNotEmpty()) {
                // Multiple candidates: prefer the one with the
                // longest prefix match (most specific).
                val best =
                    candidates.minByOrNull { key ->
                        key.length
                    }
                best?.let { bestKey ->
                    resolvedNames[bestKey]?.let {
                            generatedName ->
                        return ClassName(
                            packageName,
                            dataClassName,
                            generatedName,
                        )
                    }
                }
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
