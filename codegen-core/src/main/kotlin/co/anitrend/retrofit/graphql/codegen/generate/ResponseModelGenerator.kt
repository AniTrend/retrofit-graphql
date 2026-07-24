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
 * Property resolution uses exact [ResponseModelIdentity] matching via
 * the child's response path and runtime path derived from the current
 * identity. No heuristic fallback searches are used.
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

    fun generate(
        operation: GraphQLOperationInfo,
        selectionSet: ResponseSelectionSet,
        packageName: String,
        dataClassName: String = "${operation.name}Data",
    ): List<FileSpec> {
        val collected = linkedMapOf<ResponseModelIdentity, ProjectedSelectionSet>()
        projectAndCollect(
            selectionSet = selectionSet,
            runtimePath = RuntimePath.EMPTY,
            collected = collected,
        )

        val allNestedSpecs = mutableListOf<TypeSpec>()

        val abstractEntries = collected.filter { (_, projSet) ->
            schemaIndex.isAbstractType(projSet.parentType)
        }

        val generatedConcreteIdentities = mutableSetOf<ResponseModelIdentity>()

        var identityToClassName = assignClassNames(collected.keys.toList())

        for ((abstractIdentity, abstractProj) in abstractEntries) {
            val abstractTypeName = abstractProj.parentType
            val interfaceName =
                identityToClassName[abstractIdentity] ?: abstractProj.parentType
            val responsePath = abstractIdentity.responsePath
            val schemaPossibleTypes = schemaIndex.possibleTypesFor(abstractTypeName)

            val concreteTypeEntries = mutableListOf<
                Pair<String, Pair<ResponseModelIdentity, ProjectedSelectionSet>>,
                >()

            for (concreteTypeName in schemaPossibleTypes) {
                // P0-2: compose the full runtime path including enclosing assignments
                val concreteRuntimePath = RuntimePath(
                    assignments = abstractIdentity.runtimePath.assignments +
                        (responsePath to concreteTypeName),
                )
                val concreteIdentity = ResponseModelIdentity(
                    responsePath = responsePath,
                    runtimePath = concreteRuntimePath,
                )

                val fromCollected = collected[concreteIdentity]
                if (fromCollected != null) {
                    generatedConcreteIdentities.add(concreteIdentity)
                    concreteTypeEntries.add(
                        concreteTypeName to (concreteIdentity to fromCollected),
                    )
                } else {
                    // P0-2: always use concreteIdentity (with enclosing context),
                    // not fall back to abstractIdentity
                    concreteTypeEntries.add(
                        concreteTypeName to (concreteIdentity to abstractProj),
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

        identityToClassName = assignClassNames(collected.keys.toList())

        for ((identity, projSet) in collected) {
            if (identity in abstractEntries) continue
            if (identity in generatedConcreteIdentities) continue
            if (identity.responsePath.isEmpty()) continue

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
                currentIdentity = rootIdentity,
            )
        }

        val rootConstructorParams = rootProjected.fields.zip(rootProperties).map { (field, prop) ->
            val paramBuilder = com.squareup.kotlinpoet.ParameterSpec.builder(prop.name, prop.type)
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

    // --- Projection + Collection ---

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

        val existing = collected[identity]
        if (existing != null) {
            collected[identity] = mergeProjectedSets(existing, projSet)
        } else {
            collected[identity] = projSet
        }

        for (field in selectionSet.fields) {
            val childSet = field.selectionSet ?: continue
            val typeName = resolveNamedType(field.outputType)
            val childPath = selectionSet.responsePath + field.responseName

            if (schemaIndex.isAbstractType(typeName)) {
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
                    collected[abstractIdentity] = mergeProjectedSets(existingAbstract, abstractProj)
                } else {
                    collected[abstractIdentity] = abstractProj
                }

                val possibleTypes = schemaIndex.possibleTypesFor(typeName)
                for (concreteType in possibleTypes) {
                    val concreteRuntimePath = RuntimePath(
                        assignments = runtimePath.assignments +
                            (childPath to concreteType),
                    )
                    projectAndCollectAbstract(
                        selectionSet = childSet,
                        runtimePath = concreteRuntimePath,
                        concreteType = concreteType,
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

    private fun projectAndCollectAbstract(
        selectionSet: ResponseSelectionSet,
        runtimePath: RuntimePath,
        concreteType: String,
        collected: LinkedHashMap<ResponseModelIdentity, ProjectedSelectionSet>,
    ) {
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

        for (field in selectionSet.fields) {
            val childSet = field.selectionSet ?: continue
            val active = isVariantActive(field.runtimePaths, runtimePath)
            if (!active) continue
            val childTypeName = resolveNamedType(field.outputType)
            val childPath = selectionSet.responsePath + field.responseName

            if (schemaIndex.isAbstractType(childTypeName)) {
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
                    collected[abstractIdentity] = mergeProjectedSets(existingAbs, abstractProj)
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

    private fun mergeActiveVariants(variants: List<ResponseFieldVariant>): ResponseFieldVariant {
        if (variants.size == 1) return variants.first()
        val base = variants.first()
        val mergedMayBeAbsent = variants.any { it.condition.mayBeAbsent }
        return base.copy(condition = SelectionCondition(mayBeAbsent = mergedMayBeAbsent))
    }

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
        return base.copy(fields = mergedFields.values.sortedBy { it.responseName })
    }

    // --- Generation ---

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
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ClassName("kotlinx.serialization", "ExperimentalSerializationApi"))
                    .build(),
            )
            .addAnnotation(
                AnnotationSpec.builder(ClassName("kotlinx.serialization.json", "JsonClassDiscriminator"))
                    .addMember("%S", "__typename")
                    .build(),
            )
            .addKdoc("Sealed interface for the GraphQL abstract type `%L`.", abstractName)

        for ((concreteTypeName, identityAndSet) in concreteIdentities) {
            val (currentIdentity, projSet) = identityAndSet
            val className = concreteTypeName

            // P0-4: No schema-field synthesis. Only use projected fields.
            val applicableFields = projSet.fields.filter { field ->
                !(field.schemaName == "__typename" && field.responseName == "__typename")
            }

            val properties = applicableFields.map { field ->
                toPropertySpec(
                    field = field,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    identityToClassName = identityToClassName,
                    currentIdentity = currentIdentity,
                )
            }

            val constructorParams = applicableFields.zip(properties).map { (field, prop) ->
                val paramBuilder = com.squareup.kotlinpoet.ParameterSpec.builder(prop.name, prop.type)
                if (field.condition.mayBeAbsent && prop.type.isNullable) {
                    paramBuilder.defaultValue("null")
                }
                paramBuilder.build()
            }

            val subtypeSpec = if (applicableFields.isEmpty()) {
                // P0-4: Unselected type — emit empty serializable regular class
                TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.PUBLIC)
                    .addAnnotation(SERIALIZABLE)
                    .addAnnotation(
                        AnnotationSpec.builder(SERIAL_NAME)
                            .addMember("%S", concreteTypeName)
                            .build(),
                    )
                    .addSuperinterface(ClassName(packageName, dataClassName, interfaceName))
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
                    .addSuperinterface(ClassName(packageName, dataClassName, interfaceName))
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

        val currentIdentity = ResponseModelIdentity(
            responsePath = projectedSet.responsePath,
            runtimePath = projectedSet.runtimePath,
        )

        val properties = sortedFields.map { field ->
            toPropertySpec(
                field = field,
                dataClassName = dataClassName,
                packageName = packageName,
                identityToClassName = identityToClassName,
                currentIdentity = currentIdentity,
            )
        }

        val constructorParams = sortedFields.zip(properties).map { (field, prop) ->
            val paramBuilder = com.squareup.kotlinpoet.ParameterSpec.builder(prop.name, prop.type)
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

    // --- Property spec ---

    private fun toPropertySpec(
        field: ProjectedField,
        dataClassName: String,
        packageName: String,
        identityToClassName: Map<ResponseModelIdentity, String>,
        currentIdentity: ResponseModelIdentity,
    ): PropertySpec {
        var kotlinType = resolveTypeName(
            graphQLType = field.outputType,
            dataClassName = dataClassName,
            packageName = packageName,
            identityToClassName = identityToClassName,
            fieldName = field.responseName,
            currentIdentity = currentIdentity,
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

    // P0-1: use currentIdentity instead of currentRuntimePath
    private fun resolveTypeName(
        graphQLType: GraphQLType,
        dataClassName: String,
        packageName: String,
        identityToClassName: Map<ResponseModelIdentity, String>,
        fieldName: String,
        currentIdentity: ResponseModelIdentity,
    ): TypeName {
        return when (graphQLType) {
            is GraphQLType.Named -> {
                val base = resolveNamedTypeName(
                    name = graphQLType.name,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    identityToClassName = identityToClassName,
                    fieldName = fieldName,
                    currentIdentity = currentIdentity,
                )
                if (graphQLType.nullable) base.copy(nullable = true) else base
            }
            is GraphQLType.List -> {
                val elementType = resolveTypeName(
                    graphQLType = graphQLType.of,
                    dataClassName = dataClassName,
                    packageName = packageName,
                    identityToClassName = identityToClassName,
                    fieldName = fieldName,
                    currentIdentity = currentIdentity,
                )
                val listType = ClassName("kotlin.collections", "List")
                    .parameterizedBy(elementType)
                if (graphQLType.nullable) listType.copy(nullable = true) else listType
            }
        }
    }

    /**
     * Resolves a single named GraphQL type to a KotlinPoet [TypeName]
     * using exact [ResponseModelIdentity] matching.
     *
     * P0-1: No heuristic fallback searches. Constructs the exact child
     * identity from [currentIdentity] and looks it up directly.
     */
    private fun resolveNamedTypeName(
        name: String,
        dataClassName: String,
        packageName: String,
        identityToClassName: Map<ResponseModelIdentity, String>,
        fieldName: String,
        currentIdentity: ResponseModelIdentity,
    ): TypeName {
        // 1. Custom scalar mappings
        scalarMappings[name]?.let { return parseFqcnToTypeName(it) }

        // 2. Built-in scalars
        BUILT_IN_SCALARS[name]?.let { return parseFqcnToTypeName(it) }

        // 3. Construct exact child identity
        val childResponsePath = currentIdentity.responsePath + fieldName

        // Compute relevant runtime path assignments: only keep assignments
        // whose paths are a prefix of childResponsePath.
        val relevantAssignments = currentIdentity.runtimePath.assignments.filterKeys { path ->
            path.size <= childResponsePath.size &&
                childResponsePath.subList(0, path.size) == path
        }

        // 3. Schema-defined enums and input objects — no identity lookup needed
        val def = schemaIndex.definition(name)
        when (def) {
            is SchemaType.InputObject,
            is SchemaType.Enum,
            -> return ClassName(packageName, name)
            is SchemaType.Scalar -> error("Unknown scalar type '$name'. Add a scalar mapping.")
            null -> error("Unknown type '$name' is not defined in the schema.")
            else -> { /* continue to identity lookup */ }
        }

        val exactChildRuntimePath = RuntimePath(assignments = relevantAssignments)
        val exactChildIdentity = ResponseModelIdentity(
            responsePath = childResponsePath,
            runtimePath = exactChildRuntimePath,
        )

        val generatedName = requireNotNull(identityToClassName[exactChildIdentity]) {
            val typeLabel = def?.let { it::class.simpleName } ?: "unknown"
            "No generated model for $exactChildIdentity " +
                "(schema type '$name' is $typeLabel). " +
                "This indicates a missing projection."
        }
        return ClassName(packageName, dataClassName, generatedName)
    }

    // --- Naming helpers ---

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
                result[identity] = "${className}${count + 1}"
                seen[className] = count + 1
            }
        }

        return result
    }

    private fun identityToDottedString(identity: ResponseModelIdentity): String {
        val path = identity.responsePath.joinToString(".")
        val firstAssignment = identity.runtimePath.assignments.entries.firstOrNull()
        return if (firstAssignment != null) {
            "${firstAssignment.value}.$path"
        } else {
            path
        }
    }

    private fun dottedToPascalCase(identity: String): String =
        identity.split(".", "[", "]", "_", ":")
            .filter { it.isNotEmpty() }
            .joinToString("") {
                it.replaceFirstChar { c -> c.uppercase() }
            }

    // --- Utility ---

    private fun resolveNamedType(type: GraphQLType): String = when (type) {
        is GraphQLType.Named -> type.name
        is GraphQLType.List -> resolveNamedType(type.of)
    }

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
