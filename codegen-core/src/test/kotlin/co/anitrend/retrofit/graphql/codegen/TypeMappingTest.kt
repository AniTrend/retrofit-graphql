package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.mapping.GraphQLTypeMapper
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeMappingTest {
    private val emptySchemaIndex = SchemaIndex.EMPTY

    // ---------------------------------------------------------------------------
    // List variable types
    // ---------------------------------------------------------------------------

    @Test
    fun `should map non-null list of non-null String`() {
        // [String!]! -> non-null list of non-null strings
        val type = GraphQLType.List(
            of = GraphQLType.Named(name = "String", nullable = false),
            nullable = false,
        )

        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)

        assertTrue("Result should be ParameterizedTypeName", result is ParameterizedTypeName)
        assertFalse("List should be non-nullable", result.isNullable)

        val listType = result as ParameterizedTypeName
        assertEquals("kotlin.collections.List", listType.rawType.canonicalName)

        val elementType = listType.typeArguments.single()
        assertTrue("Element type should be ClassName", elementType is ClassName)
        assertFalse("Element type should be non-nullable", elementType.isNullable)
        assertEquals("kotlin.String", (elementType as ClassName).canonicalName)
    }

    @Test
    fun `should map non-null list of nullable Int`() {
        // [Int]! -> non-null list of nullable Int
        val type = GraphQLType.List(
            of = GraphQLType.Named(name = "Int", nullable = true),
            nullable = false,
        )

        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)

        assertTrue("Result should be ParameterizedTypeName", result is ParameterizedTypeName)
        assertFalse("List should be non-nullable", result.isNullable)

        val listType = result as ParameterizedTypeName
        val elementType = listType.typeArguments.single()
        assertTrue("Element should be nullable (Int → Int?)", elementType.isNullable)
        assertEquals("kotlin.Int", (elementType as ClassName).canonicalName)
    }

    @Test
    fun `should map nested nullable List of nullable Boolean`() {
        // [[Boolean]] -> nullable list of nullable list of nullable Boolean
        val innerType = GraphQLType.Named(name = "Boolean", nullable = true)
        val middleType = GraphQLType.List(of = innerType, nullable = true)
        val type = GraphQLType.List(of = middleType, nullable = true)

        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)

        assertTrue("Outer result should be ParameterizedTypeName", result is ParameterizedTypeName)
        assertTrue("Outer list should be nullable", result.isNullable)

        val outerList = result as ParameterizedTypeName
        assertEquals("kotlin.collections.List", outerList.rawType.canonicalName)

        val middleElement = outerList.typeArguments.single()
        assertTrue("Middle should be ParameterizedTypeName", middleElement is ParameterizedTypeName)
        assertTrue("Middle list should be nullable", middleElement.isNullable)

        val middleList = middleElement as ParameterizedTypeName
        assertEquals("kotlin.collections.List", middleList.rawType.canonicalName)

        val innerElement = middleList.typeArguments.single()
        assertTrue("Inner type should be ClassName", innerElement is ClassName)
        assertTrue("Inner Boolean should be nullable", innerElement.isNullable)
        assertEquals("kotlin.Boolean", (innerElement as ClassName).canonicalName)
    }

    // ---------------------------------------------------------------------------
    // Custom scalar mapping
    // ---------------------------------------------------------------------------

    @Test
    fun `should map custom scalar DateTime to mapped Kotlin type`() {
        val scalarMappings = mapOf("DateTime" to "java.time.Instant")
        val type = GraphQLType.Named(name = "DateTime", nullable = false)

        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", scalarMappings, emptySchemaIndex)

        assertTrue("Result should be ClassName", result is ClassName)
        assertFalse("Type should be non-nullable", result.isNullable)
        assertEquals("java.time.Instant", (result as ClassName).canonicalName)
    }

    @Test
    fun `should map nullable custom scalar to nullable Kotlin type`() {
        val scalarMappings = mapOf("DateTime" to "java.time.Instant")
        val type = GraphQLType.Named(name = "DateTime", nullable = true)

        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", scalarMappings, emptySchemaIndex)

        assertTrue("Result should be ClassName", result is ClassName)
        assertTrue("Type should be nullable", result.isNullable)
        assertEquals("java.time.Instant", (result as ClassName).canonicalName)
    }

    @Test
    fun `should map custom scalar with nested class FQCN`() {
        val scalarMappings = mapOf("Upload" to "okhttp3.MultipartBody.Part")
        val type = GraphQLType.Named(name = "Upload", nullable = false)

        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", scalarMappings, emptySchemaIndex)

        assertTrue("Result should be ClassName", result is ClassName)
        assertEquals(
            "okhttp3.MultipartBody.Part",
            (result as ClassName).canonicalName
        )
    }

    // ---------------------------------------------------------------------------
    // Built-in scalars
    // ---------------------------------------------------------------------------

    @Test
    fun `should map built-in String`() {
        val type = GraphQLType.Named(name = "String", nullable = false)
        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)

        assertTrue(result is ClassName)
        assertFalse(result.isNullable)
        assertEquals("kotlin.String", (result as ClassName).canonicalName)
    }

    @Test
    fun `should map built-in Int`() {
        val type = GraphQLType.Named(name = "Int", nullable = false)
        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)

        assertTrue(result is ClassName)
        assertFalse(result.isNullable)
        assertEquals("kotlin.Int", (result as ClassName).canonicalName)
    }

    @Test
    fun `should map built-in Float to kotlin Double`() {
        val type = GraphQLType.Named(name = "Float", nullable = false)
        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)

        assertTrue(result is ClassName)
        assertFalse(result.isNullable)
        assertEquals("kotlin.Double", (result as ClassName).canonicalName)
    }

    @Test
    fun `should map built-in Boolean`() {
        val type = GraphQLType.Named(name = "Boolean", nullable = false)
        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)

        assertTrue(result is ClassName)
        assertFalse(result.isNullable)
        assertEquals("kotlin.Boolean", (result as ClassName).canonicalName)
    }

    @Test
    fun `should map built-in ID to kotlin String`() {
        val type = GraphQLType.Named(name = "ID", nullable = false)
        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)

        assertTrue(result is ClassName)
        assertFalse(result.isNullable)
        assertEquals("kotlin.String", (result as ClassName).canonicalName)
    }

    @Test
    fun `should map built-in scalars as nullable when type is nullable`() {
        val type = GraphQLType.Named(name = "String", nullable = true)
        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)

        assertTrue(result is ClassName)
        assertTrue("Nullable String should be nullable", result.isNullable)
        assertEquals("kotlin.String", (result as ClassName).canonicalName)
    }

    // ---------------------------------------------------------------------------
    // Schema type names
    // ---------------------------------------------------------------------------

    @Test
    fun `should resolve schema-defined input types by name`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.InputObject(name = "MyInput", fields = emptyList()),
                    SchemaType.InputObject(name = "UserFilter", fields = emptyList()),
                ),
            )
        val type = GraphQLType.Named(name = "MyInput", nullable = false)

        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), schemaIndex)

        assertTrue(result is ClassName)
        assertEquals("MyInput", (result as ClassName).simpleName)
    }

    @Test
    fun `should resolve schema-defined enum types by name`() {
        val schemaIndex = SchemaIndex.from(listOf(SchemaType.Enum(name = "Status", values = listOf("OPEN"))))
        val type = GraphQLType.Named(name = "Status", nullable = false)

        val result = GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), schemaIndex)

        assertTrue(result is ClassName)
        assertEquals("Status", (result as ClassName).simpleName)
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw for schema-defined scalar without mapping`() {
        val schemaIndex = SchemaIndex.from(listOf(SchemaType.Scalar(name = "DateTime")))
        val type = GraphQLType.Named(name = "DateTime", nullable = false)

        GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), schemaIndex)
    }

    // ---------------------------------------------------------------------------
    // Unknown scalar
    // ---------------------------------------------------------------------------

    @Test(expected = IllegalStateException::class)
    fun `should throw for unknown scalar without mapping`() {
        val type = GraphQLType.Named(name = "UnknownScalar", nullable = false)
        GraphQLTypeMapper.toKotlinType(type, "test.pkg", emptyMap(), emptySchemaIndex)
    }
}
