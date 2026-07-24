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
import co.anitrend.retrofit.graphql.codegen.model.ProjectedField
import co.anitrend.retrofit.graphql.codegen.model.ProjectedSelectionSet
import co.anitrend.retrofit.graphql.codegen.model.ResponseFieldVariant
import co.anitrend.retrofit.graphql.codegen.model.ResponseModelIdentity
import co.anitrend.retrofit.graphql.codegen.model.ResponsePath
import co.anitrend.retrofit.graphql.codegen.model.ResponseSelectionSet
import co.anitrend.retrofit.graphql.codegen.model.RuntimePath
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import co.anitrend.retrofit.graphql.codegen.model.SelectionCondition
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
 * [ResponseSelectionSet] produced by
 * [co.anitrend.retrofit.graphql.codegen.parser.ResponseSelectionParser].
 *
 * The generator uses three phases:
 * 1. **Projection**: For each concrete runtime path, a selection set is
 *    projected to produce a [ProjectedSelectionSet] where each JSON
 *    response key maps to exactly one projected field.
 * 2. **Collection**: All projected selection sets are collected into a
 *    map keyed by [ResponseModelIdentity].
 * 3. **Generation**: KotlinPoet types are generated from the collected
 *    projected selection sets.
 *
 * @property schemaIndex Indexed schema metadata used to resolve type definitions.
 * @property scalarMappings Custom scalar type to Kotlin type mappings.
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

        /**
         * Tests whether a field variant is active for the given runtime path.
         */
        fun isVariantActive(
            variantPaths: Set<RuntimePath>,
            runtimePath: RuntimePath,
        ): Boolean {
            if (variantPaths.isEmpty()) return true
            return variantPaths.any { required ->
                required.assignments.all { (path, type) ->
                    runtimePath.assignments[path] == type
                }
            }
        }
    }

    /**
     * Generates kotlinx-serializable response model classes for a single
     * operation from its normalized response selection set.
     */
    fun generate(
        operation: GraphQLOperationInfo,
        selectionSet: ResponseSelectionSet,
        packageName: String,
        dataClassName: String = "${operation.name}Data",
    ): List<FileSpec> {
        // Phase 1+2: Project and collect
        val collected = linkedMapOf<ResponseModelIdentity, ProjectedSelectionSet>()
        projectAndCollect(
            selectionSet = selectionSet,
            runtimePath = RuntimePath.EMPTY,
            collected = collected,
        )

        // Phase 3: Generate model classes
        val allNestedSpecs = mutableListOf<TypeSpec>()

        // Identify abstract entries and their concrete counterparts
        val abstractEntries = collected.filter { (_, projSet) ->
            schemaIndex.isAbstractType(projSet.parentType)
        }

        val generatedConcreteIdentities = mutableSetOf<ResponseModelIdentity>()

        // Pre-compute class names for the initial identities
        var identityToClassName = assignClassNames(collected.keys.toList())

        for ((abstractIdentity, abstractProj) in abstractEntries) {
            val abstractTypeName = abstractProj.parentType
            val interfaceName = identityToClassName[abstractIdentity] ?: abstractProj.parentType
            val responsePath = abstractIdentity.responsePath
            val schemaPossibleTypes = schemaIndex.possibleTypesFor(abstractTypeName)

            val concreteTypeEntries = mutableListOf<
                Pair<String, Pair<ResponseModelIdentity, ProjectedSelectionSet>>,
                >()

            for (concreteTypeName in schemaPossibleTypes) {
                val concreteIdentity = collected.keys.find { id ->
                    id.responsePath == responsePath &&
                        id.runtimePath.assignments[responsePath] == concreteTypeName
                }

                if (concreteIdentity != null) {
                    val concreteProj = collected[concreteIdentity]!!
                    generatedConcreteIdentities.add(concreteIdentity)
                    concreteTypeEntries.add(
                        concreteTypeName to (concreteIdentity to concreteProj),
                    )
                } else {
                    // Unselected type: add empty entry
                    concreteTypeEntries.add(
                        concreteTypeName to (abstractIdentity to abstractProj),
                    )
                }
            }

            val sealedSpec = generateSealedInterface(
                abstractName = abstractTypeName,
                interfaceName = interfaceName,
                concreteIdentities = concreteTypeEntries,
                dataClassName = dataClassName,
                packageName = packageName,
                identityToClassName = identityToClassName,
                collected = collected,
            )
            allNestedSpecs.add(sealedSpec)
        }

        // Refresh class names to include identities added during sealed
        // interface generation for unselected concrete types.
        identityToClassName = assignClassNames(collected.keys.toList())

        // Generate data classes for remaining concrete identities
        for ((identity, projSet) in collected) {
            if (identity in abstractEntries) continue
            if (identity in generatedConcreteIdentities) continue
            if (identity.responsePath.isEmpty()) continue // skip root identity

            val className = identityToClassName[identity] ?: continue
            val dataSpec = generateDataClass(
                className = className,
                projectedSet = projSet,
                dataClassName = dataClassName,
                packageName = packageName,
                identityToClassName = identityToClassName,
                collected = collected,
            )
            allNestedSpecs.add(dataSpec)
        }

        // Generate root data class
        val rootIdentity = ResponseModelIdentity(
            responsePath = selectionSet.responsePath,
            runtimePath = RuntimePath.EMPTY,
        )
        val rootProjected = collected[rootIdentity] ?: projectRaw(
            selectionSet = selectionSet,
            runtimePath = RuntimePath.EMPTY,
        )

        val rootProperties = rootProjected.fields.map { field ->
            toPropertySpec(
                field = field,
                dataClassName = dataClassName,
                packageName = packageName,
                identityToClassName = identityToClassName,
                collected = collected,
                currentRuntimePath = RuntimePath.EMPTY,
            )
        }

        val rootConstructorParams = rootProjected.fields.zip(rootProperties).map { (field, prop) ->
            val paramBuilder =
                com.squareup.kotlinpoet.ParameterSpec.builder(prop.name, prop.type)
            if (field.condition.mayBeAbsent && prop.type.isNullable) {
                paramBuilder.defaultValue("null")
            }
            paramBuilder.build()
        }

        val rootTypeSpec = TypeSpec.classBuilder(dataClassName)
            .addModifiers(KModifier.PUBLIC, KModifier.DATA)
            .addAnnotation(SERIALIZABLE)
            .primaryConstructor(
                com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                    .addParameters(rootConstructorParams)
                    .build(),
            )
            .apply {
                rootProperties.forEach { prop ->
                    val builder = prop.toBuilder()
                    builder.initializer(prop.name)
                    addProperty(builder.build())
                }
                allNestedSpecs.forEach { addType(it) }
            }
            .build()

        return listOf(
            FileSpec.builder(packageName, dataClassName)
                .addType(rootTypeSpec)
                .build(),
        )
    }

    // --- Projection + Collection (combined pass) ---

    /**
     * Projects a raw [ResponseSelectionSet] onto a runtime path and
     * collects the result. For abstract types, recursively expands
     * across all possible concrete types.
     */
    private fun projectAndCollect(
        selectionSet: ResponseSelectionSet,
        runtimePath: RuntimePath,
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
    ) {
        val projSet = projectRaw(selectionSet, runtimePath)

        val identity = ResponseModelIdentity(
            responsePath = selectionSet.responsePath,
            runtimePath = runtimePath,
        )

        // Merge with existing
        val existing = collected[identity]
        if (existing != null) {
            collected[identity] = mergeProjectedSets(existing, projSet)
        } else {
            collected[identity] = projSet
        }

        // Recurse into child fields
        for (field in selectionSet.fields) {
            val childSet = field.selectionSet ?: continue
            val typeName = resolveNamedType(field.outputType)
            val childPath = selectionSet.responsePath + field.responseName

            if (schemaIndex.isAbstractType(typeName)) {
                // Also collect the abstract type itself (for sealed interface)
                val abstractProj = ProjectedSelectionSet(
                    parentType = typeName,
                    responsePath = childPath,
                    runtimePath = runtimePath,
                    fields = groupAndProjectFields(
                        childSet.fields.filter { isVariantActive(it.runtimePaths, runtimePath) },
                        runtimePath,
                    ).sortedBy { it.responseName },
                )
                val abstractIdentity = ResponseModelIdentity(
                    responsePath = childPath,
                    runtimePath = runtimePath,
                )
                val existingAbstract = collected[abstractIdentity]
                if (existingAbstract != null) {
                    collected[abstractIdentity] =
                        mergeProjectedSets(existingAbstract, abstractProj)
                } else {
                    collected[abstractIdentity] = abstractProj
                }

                // Expand across all possible concrete types
                val possibleTypes = schemaIndex.possibleTypesFor(typeName)
                for (concreteType in possibleTypes) {
                    val concreteRuntimePath = RuntimePath(
                        assignments = runtimePath.assignments +
                            (childPath to concreteType),
                    )
                    // Project the child with the concrete type context
                    projectAndCollectAbstract(
                        selectionSet = childSet,
                        runtimePath = concreteRuntimePath,
                        concreteType = concreteType,
                        collected = collected,
                    )
                }
            } else {
                // Concrete child: project and recurse
                projectAndCollect(
                    selectionSet = childSet,
                    runtimePath = runtimePath,
                    collected = collected,
                )
            }
        }
    }

    /**
     * Projects a selection set for an abstract type's concrete
     * implementor. The parentType is replaced with the concrete type
     * name so fields resolve correctly.
     */
    private fun projectAndCollectAbstract(
        selectionSet: ResponseSelectionSet,
        runtimePath: RuntimePath,
        concreteType: String,
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
    ) {
        // Select active variants for this runtime path
        val activeVariants = selectionSet.fields.filter { variant ->
            isVariantActive(variant.runtimePaths, runtimePath)
        }

        val projectedFields = groupAndProjectFields(activeVariants, runtimePath)
        val projSet = ProjectedSelectionSet(
            parentType = concreteType,
            responsePath = selectionSet.responsePath,
            runtimePath = runtimePath,
            fields = projectedFields.sortedBy { it.responseName },
        )

        val identity = ResponseModelIdentity(
            responsePath = selectionSet.responsePath,
            runtimePath = runtimePath,
        )

        val existing = collected[identity]
        if (existing != null) {
            collected[identity] = mergeProjectedSets(existing, projSet)
        } else {
            collected[identity] = projSet
        }

        // Recurse into children (both concrete and abstract)
        for (field in selectionSet.fields) {
            val childSet = field.selectionSet ?: continue
            val active = isVariantActive(field.runtimePaths, runtimePath)
            if (!active) continue
            val childTypeName = resolveNamedType(field.outputType)
            val childPath = selectionSet.responsePath + field.responseName

            if (schemaIndex.isAbstractType(childTypeName)) {
                // Also collect the abstract type itself
                val abstractProj = ProjectedSelectionSet(
                    parentType = childTypeName,
                    responsePath = childPath,
                    runtimePath = runtimePath,
                    fields = groupAndProjectFields(
                        childSet.fields.filter { isVariantActive(it.runtimePaths, runtimePath) },
                        runtimePath,
                    ).sortedBy { it.responseName },
                )
                val abstractIdentity = ResponseModelIdentity(
                    responsePath = childPath,
                    runtimePath = runtimePath,
                )
                val existingAbs = collected[abstractIdentity]
                if (existingAbs != null) {
                    collected[abstractIdentity] =
                        mergeProjectedSets(existingAbs, abstractProj)
                } else {
                    collected[abstractIdentity] = abstractProj
                }

                val possibleTypes = schemaIndex.possibleTypesFor(childTypeName)
                for (subConcrete in possibleTypes) {
                    val childRuntimePath = RuntimePath(
                        assignments = runtimePath.assignments +
                            (childPath to subConcrete),
                    )
                    projectAndCollectAbstract(
                        selectionSet = childSet,
                        runtimePath = childRuntimePath,
                        concreteType = subConcrete,
                        collected = collected,
                    )
                }
            } else {
                projectAndCollect(
                    selectionSet = childSet,
                    runtimePath = runtimePath,
                    collected = collected,
                )
            }
        }
    }

    /**
     * Projects a raw [ResponseSelectionSet] onto a runtime path.
     */
    private fun projectRaw(
        selectionSet: ResponseSelectionSet,
        runtimePath: RuntimePath,
    ): ProjectedSelectionSet {
        val activeVariants = selectionSet.fields.filter { variant ->
            isVariantActive(variant.runtimePaths, runtimePath)
        }

        val projectedFields = groupAndProjectFields(activeVariants, runtimePath)

        return ProjectedSelectionSet(
            parentType = selectionSet.parentType,
            responsePath = selectionSet.responsePath,
            runtimePath = runtimePath,
            fields = projectedFields.sortedBy { it.responseName },
        )
    }

    /**
     * Groups active variants by responseName and produces projected fields.
     */
    private fun groupAndProjectFields(
        variants: List<ResponseFieldVariant>,
        runtimePath: RuntimePath,
    ): List<ProjectedField> {
        val grouped = variants.groupBy { it.responseName }
        return grouped.map { (responseName, group) ->
            if (group.size == 1) {
                val v = group.first()
                ProjectedField(
                    responseName = v.responseName,
                    schemaName = v.schemaName,
                    outputType = v.outputType,
                    condition = v.condition,
                )
            } else {
                // Merge compatible variants for the same responseName
                val merged = mergeActiveVariants(group)
                ProjectedField(
                    responseName = merged.responseName,
                    schemaName = merged.schemaName,
                    outputType = merged.outputType,
                    condition = merged.condition,
                )
            }
        }
    }

    /**
     * Merges multiple active variants for the same responseName.
     */
    private fun mergeActiveVariants(
        variants: List<ResponseFieldVariant>,
    ): ResponseFieldVariant {
        if (variants.size == 1) return variants.first()
        val base = variants.first()
        // Merge conditions
        val mergedMayBeAbsent = variants.any { it.condition.mayBeAbsent }
        return base.copy(
            condition = SelectionCondition(mayBeAbsent = mergedMayBeAbsent),
        )
    }

    /**
     * Merges two [ProjectedSelectionSet] values.
     */
    private fun mergeProjectedSets(
        base: ProjectedSelectionSet,
        other: ProjectedSelectionSet,
    ): ProjectedSelectionSet {
        val mergedFields = linkedMapOf<String, ProjectedField>()
        for (field in (base.fields + other.fields)) {
            val existing = mergedFields[field.responseName]
            if (existing != null) {
                mergedFields[field.responseName] = existing.copy(
                    condition = SelectionCondition(
                        mayBeAbsent = existing.condition.mayBeAbsent ||
                            field.condition.mayBeAbsent,
                    ),
                )
            } else {
                mergedFields[field.responseName] = field
            }
        }
        return base.copy(
            fields = mergedFields.values.sortedBy { it.responseName },
        )
    }

    // --- Generate phase ---

    private fun generateSealedInterface(
        abstractName: String,
        interfaceName: String,
        concreteIdentities: List<Pair<String, Pair<ResponseModelIdentity, ProjectedSelectionSet>>>,
        dataClassName: String,
        packageName: String,
        identityToClassName: Map<ResponseModelIdentity, String>,
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
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
                "Sealed interface for the GraphQL abstract type `%L`.",
                abstractName,
            )

        for ((concreteTypeName, identityAndSet) in concreteIdentities) {
            val (identity, projSet) = identityAndSet
            // Use the concrete type name directly for subtype class names
            val className = concreteTypeName

            // Filter applicable fields from the projected set
            var applicableFields = projSet.fields.filter { field ->
                !(field.schemaName == "__typename" && field.responseName == "__typename")
            }

            // If the subtype has no projected fields (unselected), include
            // fields from the abstract type's schema definition
            if (applicableFields.isEmpty()) {
                val abstractDef = schemaIndex.definition(abstractName)
                if (abstractDef is SchemaType.InterfaceType) {
                    applicableFields = abstractDef.fields.map { schemaField ->
                        // Create a projected identity for this field
                        val childPath = identity.responsePath + schemaField.name
                        val childRuntimePath = RuntimePath(
                            assignments = mapOf(identity.responsePath to concreteTypeName),
                        )
                        val childProj = ProjectedSelectionSet(
                            parentType = resolveNamedType(schemaField.type),
                            responsePath = childPath,
                            runtimePath = childRuntimePath,
                            fields = emptyList(),
                        )
                        val childIdentity = ResponseModelIdentity(
                            responsePath = childPath,
                            runtimePath = childRuntimePath,
                        )
                        val existing = collected[childIdentity]
                        if (existing == null) {
                            collected[childIdentity] = childProj
                        }
                        ProjectedField(
                            responseName = schemaField.name,
                            schemaName = schemaField.name,
                            outputType = schemaField.type,
                            condition = SelectionCondition.UNCONDITIONAL,
                        )
                    }
                }
            }

            val concreteRuntimePath = RuntimePath(
                assignments = mapOf(projSet.responsePath to concreteTypeName),
            )
            val properties = applicableFields.map { field ->
                toPropertySpec(
                    field = field,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    identityToClassName = identityToClassName,
                    collected = collected,
                    currentRuntimePath = concreteRuntimePath,
                )
            }

            val constructorParams = applicableFields.zip(properties).map { (field, prop) ->
                val paramBuilder =
                    com.squareup.kotlinpoet.ParameterSpec
                        .builder(prop.name, prop.type)
                if (field.condition.mayBeAbsent && prop.type.isNullable) {
                    paramBuilder.defaultValue("null")
                }
                paramBuilder.build()
            }

            val subtypeSpec = if (applicableFields.isEmpty()) {
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

    private fun generateDataClass(
        className: String,
        projectedSet: ProjectedSelectionSet,
        dataClassName: String,
        packageName: String,
        identityToClassName: Map<ResponseModelIdentity, String>,
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
    ): TypeSpec {
        val sortedFields = projectedSet.fields.sortedBy { it.responseName }

        val properties = sortedFields.map { field ->
            toPropertySpec(
                field = field,
                dataClassName = dataClassName,
                packageName = packageName,
                identityToClassName = identityToClassName,
                collected = collected,
                currentRuntimePath = projectedSet.runtimePath,
            )
        }

        val constructorParams = sortedFields.zip(properties).map { (field, prop) ->
            val paramBuilder =
                com.squareup.kotlinpoet.ParameterSpec
                    .builder(prop.name, prop.type)
            if (field.condition.mayBeAbsent && prop.type.isNullable) {
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
     * Generates a [PropertySpec] from a [ProjectedField].
     */
    private fun toPropertySpec(
        field: ProjectedField,
        dataClassName: String,
        packageName: String,
        identityToClassName: Map<ResponseModelIdentity, String>,
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
        currentRuntimePath: RuntimePath,
    ): PropertySpec {
        var kotlinType = resolveTypeName(
            graphQLType = field.outputType,
            dataClassName = dataClassName,
            packageName = packageName,
            identityToClassName = identityToClassName,
            collected = collected,
            fieldName = field.responseName,
            currentRuntimePath = currentRuntimePath,
        )

        if (field.condition.mayBeAbsent && !kotlinType.isNullable) {
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
     * Resolves a [GraphQLType] to a KotlinPoet [TypeName].
     */
    private fun resolveTypeName(
        graphQLType: GraphQLType,
        dataClassName: String,
        packageName: String,
        identityToClassName: Map<ResponseModelIdentity, String>,
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
        fieldName: String,
        currentRuntimePath: RuntimePath,
    ): TypeName {
        return when (graphQLType) {
            is GraphQLType.Named -> {
                val base = resolveNamedTypeName(
                    name = graphQLType.name,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    identityToClassName = identityToClassName,
                    collected = collected,
                    fieldName = fieldName,
                    currentRuntimePath = currentRuntimePath,
                )
                if (graphQLType.nullable) base.copy(nullable = true) else base
            }
            is GraphQLType.List -> {
                val elementType = resolveTypeName(
                    graphQLType = graphQLType.of,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    identityToClassName = identityToClassName,
                    collected = collected,
                    fieldName = fieldName,
                    currentRuntimePath = currentRuntimePath,
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
     * Uses exact [ResponseModelIdentity] matching. For the child field,
     * the identity is: responsePath = currentPath + fieldName,
     * runtimePath = currentRuntimePath (filtered to relevant assignments).
     */
    private fun resolveNamedTypeName(
        name: String,
        dataClassName: String,
        packageName: String,
        identityToClassName: Map<ResponseModelIdentity, String>,
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
        fieldName: String,
        currentRuntimePath: RuntimePath,
    ): TypeName {
        // 1. Custom scalar mappings
        scalarMappings[name]?.let { return parseFqcnToTypeName(it) }

        // 2. Built-in scalars
        BUILT_IN_SCALARS[name]?.let { return parseFqcnToTypeName(it) }

        // 3. Look up by exact ResponseModelIdentity
        // Construct the child response path
        val parentPath = currentRuntimePath.assignments.keys.lastOrNull()
            ?: emptyList()
        val childPath = if (parentPath.isEmpty()) {
            listOf(fieldName)
        } else {
            findChildPath(collected, fieldName)
        }

        // Find the identity that matches this field
        val matchingIdentity = findMatchingIdentity(
            collected = collected,
            currentRuntimePath = currentRuntimePath,
            fieldName = fieldName,
            fieldTypeName = name,
        )

        if (matchingIdentity != null) {
            identityToClassName[matchingIdentity]?.let { generatedName ->
                return ClassName(packageName, dataClassName, generatedName)
            }
        }

        // 4. Fall back to looking up by response path in collected keys
        val fallbackIdentity = collected.keys.find { id ->
            val lastSegment = id.responsePath.lastOrNull()
            lastSegment == fieldName &&
                id.runtimePath.assignments == currentRuntimePath.assignments
        }
        fallbackIdentity?.let { id ->
            identityToClassName[id]?.let { generatedName ->
                return ClassName(packageName, dataClassName, generatedName)
            }
        }

        // 5. Schema-defined type
        val def = schemaIndex.definition(name)
        when (def) {
            is SchemaType.ObjectType -> {
                // Try to find by response path suffix (last segment is fieldName)
                val suffixMatch = collected.keys.find { id ->
                    id.responsePath.lastOrNull() == fieldName &&
                        id.runtimePath.assignments.all { (path, type) ->
                            currentRuntimePath.assignments[path]?.let {
                                it == type
                            } ?: true
                        }
                }
                suffixMatch?.let { id ->
                    identityToClassName[id]?.let { generatedName ->
                        return ClassName(packageName, dataClassName, generatedName)
                    }
                }
                // Fallback: match by response path suffix only (ignoring runtime path)
                val pathOnlyMatch = collected.keys.find { id ->
                    id.responsePath.lastOrNull() == fieldName
                }
                pathOnlyMatch?.let { id ->
                    identityToClassName[id]?.let { generatedName ->
                        return ClassName(packageName, dataClassName, generatedName)
                    }
                }
                error(
                    "Object type '$name' for field '$fieldName' has no " +
                        "generated model. This indicates a missing projection.",
                )
            }
            is SchemaType.InterfaceType,
            is SchemaType.UnionType,
            -> {
                // For abstract types, look up the abstract identity
                val absIdentity = collected.keys.find { id ->
                    id.responsePath.lastOrNull() == fieldName &&
                        id.runtimePath.assignments.isEmpty()
                }
                absIdentity?.let { id ->
                    identityToClassName[id]?.let { generatedName ->
                        return ClassName(packageName, dataClassName, generatedName)
                    }
                }
                error(
                    "Abstract type '$name' for field '$fieldName' was not " +
                        "collected. This indicates a processing error.",
                )
            }
            is SchemaType.InputObject,
            is SchemaType.Enum,
            -> {
                return ClassName(packageName, name)
            }
            is SchemaType.Scalar -> {
                error(
                    "Unknown scalar type '$name'. " +
                        "Add a scalar mapping.",
                )
            }
            null -> {
                error(
                    "Unknown type '$name' is not defined in the schema.",
                )
            }
        }
    }

    /**
     * Finds the response path for a child field by looking through
     * collected identities.
     */
    private fun findChildPath(
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
        fieldName: String,
    ): ResponsePath {
        val match = collected.keys.find { id ->
            id.responsePath.lastOrNull() == fieldName
        }
        return match?.responsePath ?: listOf(fieldName)
    }

    /**
     * Finds the [ResponseModelIdentity] that exactly matches a field
     * given its enclosing runtime path.
     */
    private fun findMatchingIdentity(
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
        currentRuntimePath: RuntimePath,
        fieldName: String,
        fieldTypeName: String,
    ): ResponseModelIdentity? {
        // Try exact match first: same runtime path assignments,
        // response path ends with fieldName
        val exact = collected.keys.find { id ->
            id.responsePath.lastOrNull() == fieldName &&
                id.runtimePath == currentRuntimePath
        }
        if (exact != null) return exact

        // Try match where runtime path is a subset (abstract field
        // inside a concrete subtype)
        val subset = collected.keys.find { id ->
            id.responsePath.lastOrNull() == fieldName &&
                id.runtimePath.assignments.any() &&
                currentRuntimePath.assignments.all { (path, type) ->
                    id.runtimePath.assignments[path] == type
                }
        }
        if (subset != null) return subset

        // Try match by response path ending
        val pathMatch = collected.keys.find { id ->
            id.responsePath.lastOrNull() == fieldName &&
                id.runtimePath.assignments.isEmpty()
        }
        if (pathMatch != null) return pathMatch

        return null
    }

    /**
     * Assigns generated class names from [ResponseModelIdentity] keys.
     */
    private fun assignClassNames(
        identities: List<ResponseModelIdentity>,
    ): Map<ResponseModelIdentity, String> {
        val result = linkedMapOf<ResponseModelIdentity, String>()
        val seen = mutableMapOf<String, Int>()

        for (identity in identities) {
            val identifier = identityToDottedString(identity)
            val className = dottedToPascalCase(identifier)
            val count = seen.getOrDefault(className, 0)
            if (count == 0) {
                result[identity] = className
                seen[className] = 1
            } else {
                val uniqueName = "${className}${count + 1}"
                result[identity] = uniqueName
                seen[className] = count + 1
            }
        }

        return result
    }

    /**
     * Converts a [ResponseModelIdentity] to a dotted string for
     * deterministic class name generation.
     */
    private fun identityToDottedString(identity: ResponseModelIdentity): String {
        val path = identity.responsePath.joinToString(".")
        // For identities with runtime type assignments, prefix with
        // the first concrete type to disambiguate (e.g., "Success.result.detail")
        val firstAssignment = identity.runtimePath.assignments.entries.firstOrNull()
        return if (firstAssignment != null) {
            "${firstAssignment.value}.$path"
        } else {
            path
        }
    }

    /**
     * Converts a dotted identity string to PascalCase.
     */
    private fun dottedToPascalCase(identity: String): String =
        identity.split(".", "[", "]", "_", ":")
            .filter { it.isNotEmpty() }
            .joinToString("") {
                it.replaceFirstChar { c -> c.uppercase() }
            }

    /**
     * Extracts the innermost named type from a [GraphQLType].
     */
    private fun resolveNamedType(type: GraphQLType): String {
        return when (type) {
            is GraphQLType.Named -> type.name
            is GraphQLType.List -> resolveNamedType(type.of)
        }
    }

    /**
     * Parses a fully-qualified Kotlin type name string into a [ClassName].
     */
    private fun parseFqcnToTypeName(fqcn: String): ClassName {
        val parts = fqcn.split(".")
        val firstClassIndex = parts.indexOfLast { it.first().isLowerCase() } + 1
            .coerceAtLeast(1)
        val pkg = parts.subList(0, firstClassIndex).joinToString(".")
        val classNames = parts.subList(firstClassIndex, parts.size)

        return when (classNames.size) {
            0 -> error("Invalid fully-qualified class name: $fqcn")
            1 -> ClassName(pkg, classNames[0])
            else -> {
                var className = ClassName(pkg, classNames[0])
                for (i in 1 until classNames.size) {
                    className = className.nestedClass(classNames[i])
                }
                className
            }
        }
    }
}
