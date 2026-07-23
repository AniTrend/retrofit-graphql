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

package co.anitrend.retrofit.graphql.codegen.schema

import graphql.language.ScalarTypeDefinition
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.ScalarInfo
import graphql.schema.idl.SchemaGenerator
import graphql.validation.ValidationError
import graphql.validation.Validator
import java.util.Locale

/**
 * Result of validating a GraphQL operation document against a compiled schema.
 *
 * @property isValid Whether the document passed validation.
 * @property errors Validation error messages, or empty if the document is valid.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
)

/**
 * Compiles a GraphQL schema IDL string into an executable [GraphQLSchema] using
 * graphql-java's IDL tools. Primarily used for validating GraphQL operation
 * documents against the schema during code generation.
 *
 * Custom scalars that are not part of the built-in GraphQL specification scalars
 * (String, Int, Float, Boolean, ID) are wired with pass-through coercing so the
 * schema can be built without external scalar implementations.
 *
 * @param mappedScalars Optional mapping of custom scalar names to their Kotlin
 *   type names. Reserved for future use; not currently consumed by the compiler.
 */
class SchemaCompiler(
    @Suppress("unused")
    private val mappedScalars: Map<String, String> = emptyMap(),
) {
    /**
     * Builds a graphql-java [GraphQLSchema] from a schema IDL string.
     *
     * Registers pass-through coercing for custom scalars not built into
     * graphql-java. Registers no-op data fetchers for root operation types
     * (Query, Mutation, Subscription) so the schema is executable.
     *
     * @param schemaIdl The schema definition in SDL format.
     * @return A compiled executable [GraphQLSchema].
     * @throws IllegalStateException if schema parsing or building fails.
     */
    fun compile(schemaIdl: String): GraphQLSchema {
        return try {
            val registry = graphql.schema.idl.SchemaParser().parse(schemaIdl)
            val wiring = buildRuntimeWiring(registry)
            SchemaGenerator().makeExecutableSchema(registry, wiring)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to compile GraphQL schema: ${e.message}",
                e,
            )
        }
    }

    /**
     * Validates a GraphQL operation document string against the compiled schema.
     *
     * @param schema A compiled executable [GraphQLSchema].
     * @param operationDocument The GraphQL operation (query or mutation) as a string.
     * @return [ValidationResult] indicating whether the document is valid and any errors.
     */
    fun validate(
        schema: GraphQLSchema,
        operationDocument: String,
    ): ValidationResult {
        val document =
            try {
                graphql.parser.Parser().parseDocument(operationDocument)
            } catch (e: Exception) {
                return ValidationResult(
                    isValid = false,
                    errors = listOf("Failed to parse operation document: ${e.message}"),
                )
            }

        val validationErrors: List<ValidationError> =
            Validator().validateDocument(schema, document, Locale.ENGLISH)

        return ValidationResult(
            isValid = validationErrors.isEmpty(),
            errors = validationErrors.map { it.message },
        )
    }

    private fun buildRuntimeWiring(registry: graphql.schema.idl.TypeDefinitionRegistry): RuntimeWiring {
        val wiring = RuntimeWiring.newRuntimeWiring()

        // Wire custom scalars with pass-through coercing
        for (scalarName in registry.scalars().keys) {
            if (!ScalarInfo.isGraphqlSpecifiedScalar(scalarName)) {
                wiring.scalar(buildPassThroughScalar(scalarName))
            }
        }

        // Wire no-op data fetchers for root operation types
        val rootTypes = listOf("Query", "Mutation", "Subscription")
        for (typeName in rootTypes) {
            if (registry.hasType(typeName)) {
                wiring.type(typeName) { builder ->
                    builder.defaultDataFetcher(NoOpDataFetcher())
                }
            }
        }

        // Wire no-op type resolvers for interface and union types.
        // graphql-java requires a TypeResolver for every abstract type at
        // schema build time. Since this schema is only used for validation
        // (not execution), a null-returning resolver is sufficient.
        val abstractTypes =
            registry.types().values.filter {
                it is graphql.language.InterfaceTypeDefinition ||
                    it is graphql.language.UnionTypeDefinition
            }
        for (abstractType in abstractTypes) {
            wiring.type(abstractType.name) { builder ->
                builder.typeResolver { null }
            }
        }

        return wiring.build()
    }

    private fun buildPassThroughScalar(name: String): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .name(name)
            .definition(ScalarTypeDefinition.newScalarTypeDefinition().name(name).build())
            .coercing(PassThroughCoercing)
            .build()
    }

    /**
     * No-op [graphql.schema.DataFetcher] that returns null for every field.
     */
    private class NoOpDataFetcher : graphql.schema.DataFetcher<Any?> {
        override fun get(environment: DataFetchingEnvironment): Any? = null
    }

    /**
     * Pass-through [Coercing] implementation that simply returns input values as-is.
     * Used for custom scalars whose actual serialization/deserialization is handled
     * by the Retrofit converter chain rather than by graphql-java.
     */
    private object PassThroughCoercing : Coercing<Any, Any> {
        @Throws(CoercingSerializeException::class)
        override fun serialize(
            dataFetcherResult: Any,
            graphQLContext: graphql.GraphQLContext,
            locale: Locale,
        ): Any = dataFetcherResult

        @Throws(CoercingParseValueException::class)
        override fun parseValue(
            input: Any,
            graphQLContext: graphql.GraphQLContext,
            locale: Locale,
        ): Any = input

        @Throws(CoercingParseLiteralException::class)
        override fun parseLiteral(
            input: graphql.language.Value<*>,
            coercedVariables: graphql.execution.CoercedVariables,
            graphQLContext: graphql.GraphQLContext,
            locale: Locale,
        ): Any = input
    }
}
