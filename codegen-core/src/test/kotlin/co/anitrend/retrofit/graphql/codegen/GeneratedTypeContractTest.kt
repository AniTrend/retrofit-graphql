package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.generate.EnumGenerator
import co.anitrend.retrofit.graphql.codegen.generate.InputObjectGenerator
import co.anitrend.retrofit.graphql.codegen.generate.OperationRequestGenerator
import co.anitrend.retrofit.graphql.codegen.generate.VariableClassGenerator
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedTypeContractTest {
    private val packageName = "test.pkg"

    @Test
    fun `should generate direct enum variable helpers against enum classes`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.Enum(name = "Status", values = listOf("OPEN", "CLOSED")),
                ),
            )
        val operation =
            GraphQLOperationInfo(
                name = "GetItems",
                type = OperationType.QUERY,
                document = "query GetItems",
                sourceFile = "GetItems.graphql",
                variables =
                    listOf(
                        GraphQLVariableInfo(
                            name = "status",
                            type = GraphQLType.Named(name = "Status", nullable = false),
                        ),
                    ),
            )

        val variableSource =
            VariableClassGenerator.generate(operation, packageName, emptyMap(), schemaIndex)!!
                .toString()
        val requestSource = OperationRequestGenerator.generate(operation, packageName, emptyMap(), schemaIndex).toString()
        val enumSource = EnumGenerator.generate(schemaIndex.enums, packageName).single().toString()

        assertTrue(variableSource.contains("public val status: Status"))
        assertFalse(variableSource.contains("GraphQLEnums"))
        assertTrue(requestSource.contains("status: Status"))
        assertTrue(enumSource.contains("public enum class Status"))
    }

    @Test
    fun `should generate input objects with nested enum fields`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.Enum(name = "Status", values = listOf("OPEN", "CLOSED")),
                    SchemaType.InputObject(
                        name = "FilterInput",
                        fields =
                            listOf(
                                SchemaType.InputField(
                                    name = "status",
                                    type = GraphQLType.Named(name = "Status", nullable = true),
                                ),
                            ),
                    ),
                ),
            )

        val source =
            InputObjectGenerator.generate(schemaIndex.inputObjects, packageName, emptyMap(), schemaIndex)
                .single()
                .toString()

        assertTrue(source.contains("public data class FilterInput"))
        assertTrue(source.contains("public val status: Status?"))
    }

    @Test
    fun `should honor custom scalar mappings for schema scalar variables`() {
        val schemaIndex = SchemaIndex.from(listOf(SchemaType.Scalar(name = "DateTime")))
        val operation =
            GraphQLOperationInfo(
                name = "GetItems",
                type = OperationType.QUERY,
                document = "query GetItems",
                sourceFile = "GetItems.graphql",
                variables =
                    listOf(
                        GraphQLVariableInfo(
                            name = "createdAfter",
                            type = GraphQLType.Named(name = "DateTime", nullable = false),
                        ),
                    ),
            )

        val source =
            VariableClassGenerator.generate(
                operation = operation,
                packageName = packageName,
                scalarMappings = mapOf("DateTime" to "java.time.Instant"),
                schemaIndex = schemaIndex,
            )!!.toString()

        assertTrue(source.contains("import java.time.Instant"))
        assertTrue(source.contains("public val createdAfter: Instant"))
    }
}
