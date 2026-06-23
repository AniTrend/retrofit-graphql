package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import co.anitrend.retrofit.graphql.codegen.validate.GraphQLTypeUsageValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphQLTypeUsageValidatorTest {
    @Test
    fun `should allow mapped scalars absent from schema`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.InputObject(
                        name = "UploadWrapper",
                        fields = listOf(SchemaType.InputField(name = "file", type = GraphQLType.Named(name = "Upload", nullable = false))),
                    ),
                ),
            )

        GraphQLTypeUsageValidator.validate(
            operations = emptyList(),
            schemaIndex = schemaIndex,
            scalarMappings = mapOf("Upload" to "okhttp3.MultipartBody.Part"),
        )
    }

    @Test
    fun `should report nested schema scalars with full variable path`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.Scalar(name = "DateTime"),
                    SchemaType.InputObject(
                        name = "NestedInput",
                        fields = listOf(SchemaType.InputField(name = "createdAt", type = GraphQLType.Named(name = "DateTime", nullable = false))),
                    ),
                    SchemaType.InputObject(
                        name = "FilterInput",
                        fields = listOf(SchemaType.InputField(name = "nested", type = GraphQLType.Named(name = "NestedInput", nullable = false))),
                    ),
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
                            name = "filter",
                            type = GraphQLType.Named(name = "FilterInput", nullable = false),
                        ),
                    ),
            )

        try {
            GraphQLTypeUsageValidator.validate(
                operations = listOf(operation),
                schemaIndex = schemaIndex,
                scalarMappings = emptyMap(),
            )
            throw AssertionError("Expected nested scalar validation to fail")
        } catch (exception: IllegalArgumentException) {
            assertTrue(exception.message!!.contains("Unknown scalar type 'DateTime'"))
            assertTrue(exception.message!!.contains("operation 'GetItems' variable 'filter'.nested.createdAt"))
        }
    }
}
