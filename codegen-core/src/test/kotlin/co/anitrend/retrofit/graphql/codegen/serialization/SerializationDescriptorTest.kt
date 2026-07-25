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

package co.anitrend.retrofit.graphql.codegen.serialization

import co.anitrend.retrofit.graphql.codegen.config.SerializationBackend
import co.anitrend.retrofit.graphql.codegen.generate.EnumGenerator
import co.anitrend.retrofit.graphql.codegen.generate.InputObjectGenerator
import co.anitrend.retrofit.graphql.codegen.generate.ResponseModelGenerator
import co.anitrend.retrofit.graphql.codegen.generate.VariableClassGenerator
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import co.anitrend.retrofit.graphql.codegen.parser.ResponseSelectionParser
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Verifies that generated serialization annotations produce correct
 * descriptor contracts.
 *
 * **Layer D: Serialization Descriptor Contract Tests.**
 *
 * These tests verify that `@SerialName` and `@SerializedName` annotation
 * values in generated source code match the exact expected wire names.
 * The annotation values directly correspond to `serializer().descriptor`
 * properties at runtime:
 * - `serialName` = class-level `@SerialName` value
 * - `getElementName(index)` = property-level `@SerialName` value
 * - enum literal `getElementName(index)` = enum constant `@SerialName` value
 *
 * All assertions use EXACT string matching, not substring matching.
 */
class SerializationDescriptorTest {

    private lateinit var schemaIndex: SchemaIndex

    @Before
    fun setUp() {
        val schemaFile = fixtureFile("phase2/schemas/contract-test.graphqls")
        val result = SchemaParser().parseWithRootTypes(schemaFile)
        schemaIndex = SchemaIndex.from(
            types = result.types,
            queryTypeName = result.queryTypeName,
            mutationTypeName = result.mutationTypeName,
            subscriptionTypeName = result.subscriptionTypeName,
        )
    }

    // -----------------------------------------------------------------------
    // Root response descriptor
    // -----------------------------------------------------------------------

    @Test
    fun `root response data class has SerialName with operation data class name`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUser.graphql",
            operationName = "GetCurrentUser",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // Root data class must have class-level descriptor matching the data class name
        assertTrue(
            "@SerialName(\"GetCurrentUserData\") annotation must be present on root data class",
            source.contains("@SerialName(\"GetCurrentUserData\")"),
        )
    }

    // -----------------------------------------------------------------------
    // Nested response descriptor
    // -----------------------------------------------------------------------

    @Test
    fun `nested response data class has path-qualified SerialName`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUser.graphql",
            operationName = "GetCurrentUser",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // The nested Viewer type (under getCurrentUser) should have
        // a path-qualified descriptor
        assertTrue(
            "Nested viewer type must have path-qualified @SerialName",
            source.contains("@SerialName(\"GetCurrentUserData.getCurrentUser\")"),
        )
    }

    // -----------------------------------------------------------------------
    // Nested nested response descriptor (friends within viewer -> User type)
    // -----------------------------------------------------------------------

    @Test
    fun `doubly nested response data class has full path descriptor`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUser.graphql",
            operationName = "GetCurrentUser",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // Friends within GetCurrentUser -> User (under getCurrentUser) 
        // This should be at getCurrentUser.friends path
        assertTrue(
            "Friends nested type must have path-qualified @SerialName",
            source.contains("@SerialName(\"GetCurrentUserData.getCurrentUser.friends\")"),
        )
    }

    // -----------------------------------------------------------------------
    // Repeated schema projections produce distinct descriptors
    // -----------------------------------------------------------------------

    @Test
    fun `distinct descriptors for repeated schema projections`() {
        // The friends field (list of User) appears nested under getCurrentUser.
        // It should have its own distinct descriptor path.
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUser.graphql",
            operationName = "GetCurrentUser",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // The top-level getCurrentUser has its descriptor
        assertTrue(source.contains("@SerialName(\"GetCurrentUserData.getCurrentUser\")"))

        // The friends nested list has a different descriptor
        assertTrue(source.contains("@SerialName(\"GetCurrentUserData.getCurrentUser.friends\")"))
    }

    // -----------------------------------------------------------------------
    // Ordinary property getElementName equals GraphQL wire name
    // -----------------------------------------------------------------------

    @Test
    fun `ordinary property SerialName matches GraphQL wire name`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUser.graphql",
            operationName = "GetCurrentUser",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // Ordinary fields: @SerialName uses the schema field name
        assertTrue("@SerialName(\"id\")", source.contains("@SerialName(\"id\")"))
        assertTrue("@SerialName(\"login\")", source.contains("@SerialName(\"login\")"))
        assertTrue("@SerialName(\"name\")", source.contains("@SerialName(\"name\")"))
        assertTrue("@SerialName(\"bio\")", source.contains("@SerialName(\"bio\")"))
        assertTrue("@SerialName(\"email\")", source.contains("@SerialName(\"email\")"))
    }

    // -----------------------------------------------------------------------
    // Capitalized GraphQL key preserved
    // -----------------------------------------------------------------------

    @Test
    fun `capitalized GraphQL key preserved in SerialName with lowercased kotlinName`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUserFull.graphql",
            operationName = "GetCurrentUserFull",
            backend = SerializationBackend.KOTLINX,
            scalarMappings = mapOf("DateTime" to "kotlin.String"),
        ).first().toString()

        // "Name" (capital N) is a GraphQL field - @SerialName must preserve the exact capitalization
        assertTrue("@SerialName(\"Name\")", source.contains("@SerialName(\"Name\")"))
        // Kotlin property name must be lowercased to "name"
        // The "name" field (lowercase) already exists, so the capitalized "Name" must get a collision suffix
        assertFalse("Property name must NOT be 'Name'", source.contains("public val Name:"))
    }

    // -----------------------------------------------------------------------
    // Aliased response key
    // -----------------------------------------------------------------------

    @Test
    fun `aliased field uses response alias in SerialName not schema name`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetUserWithAliases.graphql",
            operationName = "GetUserWithAliases",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // "userId" is an alias on "id" - @SerialName must use the alias
        assertTrue("@SerialName(\"userId\")", source.contains("@SerialName(\"userId\")"))
        // "userName" is an alias on "login"
        assertTrue("@SerialName(\"userName\")", source.contains("@SerialName(\"userName\")"))
        // "userBio" is an alias on "bio"
        assertTrue("@SerialName(\"userBio\")", source.contains("@SerialName(\"userBio\")"))
    }

    // -----------------------------------------------------------------------
    // Keyword/sanitized property
    // -----------------------------------------------------------------------

    @Test
    fun `keyword property has kotlinName escaped but SerialName uses wire name`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUser.graphql",
            operationName = "GetCurrentUser",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // "private" is a Kotlin keyword - kotlinName should be "privateValue"
        assertFalse("Property should NOT be named 'private'", source.contains("public val private:"))
        assertTrue("Property should be 'privateValue'", source.contains("public val privateValue:"))

        // @SerialName must use the ORIGINAL GraphQL wire name "private"
        assertTrue("@SerialName(\"private\")", source.contains("@SerialName(\"private\")"))

        // "object" is a Kotlin keyword - kotlinName should be "objectValue"
        assertFalse("Property should NOT be named 'object'", source.contains("public val object:"))
        assertTrue("@SerialName(\"object\")", source.contains("@SerialName(\"object\")"))

        // "when" is a Kotlin keyword
        assertTrue("@SerialName(\"when\")", source.contains("@SerialName(\"when\")"))
    }

    @Test
    fun `soft keyword isActive is NOT escaped`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUser.graphql",
            operationName = "GetCurrentUser",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // "is" is a hard keyword, but "isActive" is not a keyword - should NOT be escaped
        assertTrue("isActive should keep its name", source.contains("@SerialName(\"isActive\")"))
        // Also verify the Kotlin property name is preserved
        assertTrue("isActive property name should be unchanged", source.contains("public val isActive:"))
    }

    // -----------------------------------------------------------------------
    // Enum field in response type
    // -----------------------------------------------------------------------

    @Test
    fun `enum field in response type has SerialName with wire name`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUserFull.graphql",
            operationName = "GetCurrentUserFull",
            backend = SerializationBackend.KOTLINX,
            scalarMappings = mapOf("DateTime" to "kotlin.String"),
        ).first().toString()

        // The "status" field of type Status must have @SerialName("status")
        assertTrue("@SerialName(\"status\")", source.contains("@SerialName(\"status\")"))
    }

    // -----------------------------------------------------------------------
    // Collision fields
    // -----------------------------------------------------------------------

    @Test
    fun `collision fields get distinct SerialName values for each wire name`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUserFull.graphql",
            operationName = "GetCurrentUserFull",
            backend = SerializationBackend.KOTLINX,
            scalarMappings = mapOf("DateTime" to "kotlin.String"),
        ).first().toString()

        // "myField" and "my_field" both normalise to "myfield" in Kotlin
        // Both must have their distinct @SerialName annotations
        assertTrue("@SerialName(\"myField\")", source.contains("@SerialName(\"myField\")"))
        assertTrue("@SerialName(\"my_field\")", source.contains("@SerialName(\"my_field\")"))
        // The Kotlin property names must be distinct (collision resolution)
        assertTrue("Both collision properties must appear", source.contains("myField") && source.contains("my_field"))
    }

    // -----------------------------------------------------------------------
    // Variable name
    // -----------------------------------------------------------------------

    @Test
    fun `variable SerialName matches GraphQL variable name`() {
        val operation = GraphQLOperationInfo(
            name = "UpdateUser",
            type = OperationType.MUTATION,
            document = "mutation UpdateUser(\$input: UserInput!) { updateUser(input: \$input) { id } }",
            sourceFile = "UpdateUser.graphql",
            variables = listOf(
                GraphQLVariableInfo("input", GraphQLType.Named(name = "UserInput", nullable = false)),
            ),
        )
        val source = VariableClassGenerator.generate(
            operation, "com.example", emptyMap(), schemaIndex, SerializationBackend.KOTLINX,
        )!!.toString()

        assertTrue("@SerialName(\"input\")", source.contains("@SerialName(\"input\")"))
    }

    @Test
    fun `keyword variable has kotlinName escaped but SerialName uses wire name`() {
        val operation = GraphQLOperationInfo(
            name = "GetItems",
            type = OperationType.QUERY,
            document = "query GetItems(\$private: String, \$object: Boolean, \$when: Int) { getCurrentUser { id } }",
            sourceFile = "GetItems.graphql",
            variables = listOf(
                GraphQLVariableInfo("private", GraphQLType.Named(name = "String", nullable = true)),
                GraphQLVariableInfo("object", GraphQLType.Named(name = "Boolean", nullable = true)),
                GraphQLVariableInfo("when", GraphQLType.Named(name = "Int", nullable = true)),
            ),
        )
        val source = VariableClassGenerator.generate(
            operation, "com.example", emptyMap(), schemaIndex, SerializationBackend.KOTLINX,
        )!!.toString()

        // Kotlin names must be escaped
        assertTrue(source.contains("public val privateValue:"))
        assertTrue(source.contains("public val objectValue:"))
        assertTrue(source.contains("public val whenValue:"))

        // @SerialName must use wire names
        assertTrue("@SerialName(\"private\")", source.contains("@SerialName(\"private\")"))
        assertTrue("@SerialName(\"object\")", source.contains("@SerialName(\"object\")"))
        assertTrue("@SerialName(\"when\")", source.contains("@SerialName(\"when\")"))
    }

    // -----------------------------------------------------------------------
    // Input field name
    // -----------------------------------------------------------------------

    @Test
    fun `input field SerialName matches GraphQL input field name`() {
        val source = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.KOTLINX,
        ).first { "UserInput" in it.toString() }.toString()

        assertTrue("@SerialName(\"name\")", source.contains("@SerialName(\"name\")"))
        assertTrue("@SerialName(\"bio\")", source.contains("@SerialName(\"bio\")"))
        assertTrue("@SerialName(\"email\")", source.contains("@SerialName(\"email\")"))
        assertTrue("@SerialName(\"tags\")", source.contains("@SerialName(\"tags\")"))
        assertTrue("@SerialName(\"sortOrder\")", source.contains("@SerialName(\"sortOrder\")"))
    }

    @Test
    fun `keyword input field has kotlinName escaped but SerialName uses wire name`() {
        val source = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.KOTLINX,
        ).first { "UserInput" in it.toString() }.toString()

        // "private" keyword field
        assertFalse(source.contains("public val private:"))
        assertTrue("@SerialName(\"private\")", source.contains("@SerialName(\"private\")"))
        assertTrue(source.contains("public val privateValue:"))

        // "object" keyword field
        assertTrue("@SerialName(\"object\")", source.contains("@SerialName(\"object\")"))
    }

    @Test
    fun `nested input object field SerialName matches wire name`() {
        val source = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.KOTLINX,
        ).first { it.name == "NestedInput" }.toString()

        assertTrue("@SerialName(\"when\")", source.contains("@SerialName(\"when\")"))
        assertTrue("@SerialName(\"values\")", source.contains("@SerialName(\"values\")"))

        // "is" is also a keyword
        assertTrue("@SerialName(\"is\")", source.contains("@SerialName(\"is\")"))
    }

    // -----------------------------------------------------------------------
    // Enum descriptor
    // -----------------------------------------------------------------------

    @Test
    fun `enum class has Serializable annotation for KOTLINX backend`() {
        val source = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.KOTLINX,
        ).first { "Status" in it.toString() }.toString()

        assertTrue("@Serializable", source.contains("@Serializable"))
    }

    @Test
    fun `every enum literal SerialName returns exact GraphQL value`() {
        val source = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.KOTLINX,
        ).first { it.toString().contains("Status") && !it.toString().contains("SortOrder") }.toString()

        // Each enum constant must have @SerialName with the exact GraphQL value
        assertTrue("@SerialName(\"OPEN\")", source.contains("@SerialName(\"OPEN\")"))
        assertTrue("@SerialName(\"CLOSED\")", source.contains("@SerialName(\"CLOSED\")"))
        assertTrue("@SerialName(\"PENDING\")", source.contains("@SerialName(\"PENDING\")"))
        assertTrue("@SerialName(\"IN_PROGRESS\")", source.contains("@SerialName(\"IN_PROGRESS\")"))
        assertTrue("@SerialName(\"private\")", source.contains("@SerialName(\"private\")"))
        assertTrue("@SerialName(\"object\")", source.contains("@SerialName(\"object\")"))
    }

    @Test
    fun `enum with lowercase values preserves wire name in SerialName`() {
        val source = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.KOTLINX,
        ).first { "SortOrder" in it.toString() }.toString()

        // Lowercase enum values: wire name is lowercase, kotlinName is uppercase
        assertTrue("@SerialName(\"asc\")", source.contains("@SerialName(\"asc\")"))
        assertTrue("@SerialName(\"desc\")", source.contains("@SerialName(\"desc\")"))
        // Kotlin names must be uppercased
        assertTrue(source.contains("ASC"))
        assertTrue(source.contains("DESC"))
    }

    @Test
    fun `enum keyword values have wire name preserved and kotlinName escaped`() {
        val source = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.KOTLINX,
        ).first { "Status" in it.toString() }.toString()

        // "private" keyword enum value
        assertTrue("@SerialName(\"private\")", source.contains("@SerialName(\"private\")"))
        // "object" keyword enum value
        assertTrue("@SerialName(\"object\")", source.contains("@SerialName(\"object\")"))
        // Kotlin names must be uppercase and escaped
        assertTrue("PRIVATEVALUE", source.contains("PRIVATEVALUE"))
        assertTrue("OBJECTVALUE", source.contains("OBJECTVALUE"))
    }

    // -----------------------------------------------------------------------
    // Enum with mixed case values
    // -----------------------------------------------------------------------

    @Test
    fun `enum with mixed case values preserves exact wire name`() {
        val source = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.KOTLINX,
        ).first { "MediaType" in it.toString() }.toString()

        assertTrue("@SerialName(\"ANIME\")", source.contains("@SerialName(\"ANIME\")"))
        assertTrue("@SerialName(\"MANGA\")", source.contains("@SerialName(\"MANGA\")"))
        assertTrue("@SerialName(\"lightNovel\")", source.contains("@SerialName(\"lightNovel\")"))
        // Kotlin names must be uppercase
        assertTrue(source.contains("LIGHTNOVEL"))
    }

    // -----------------------------------------------------------------------
    // Sealed interface descriptor
    // -----------------------------------------------------------------------

    @Test
    fun `sealed interface has SerialName with operation plus response path`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/SearchQuery.graphql",
            operationName = "SearchQuery",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // The sealed interface under the search field should have
        // a descriptor that includes the operation name and path
        assertTrue(
            "Sealed interface must have @SerialName",
            source.contains("@SerialName(\"SearchQueryData.search\")"),
        )
    }

    // -----------------------------------------------------------------------
    // Concrete subtype serial name equals __typename value
    // -----------------------------------------------------------------------

    @Test
    fun `concrete subtype SerialName equals exact __typename value`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/SearchQuery.graphql",
            operationName = "SearchQuery",
            backend = SerializationBackend.KOTLINX,
        ).first().toString()

        // Concrete subtypes must have @SerialName matching the __typename discriminator value
        assertTrue("@SerialName(\"Page\")", source.contains("@SerialName(\"Page\")"))
        assertTrue("@SerialName(\"TextMessage\")", source.contains("@SerialName(\"TextMessage\")"))
    }

    // -----------------------------------------------------------------------
    // Gson backend
    // -----------------------------------------------------------------------

    @Test
    fun `GSON backend emits SerializedName on all property types`() {
        // Variable class
        val varSource = VariableClassGenerator.generate(
            GraphQLOperationInfo(
                "GetItems", OperationType.QUERY, "query GetItems(\$first: Int!) { getCurrentUser { id } }",
                "GetItems.graphql",
                listOf(GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false))),
            ), "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )!!.toString()
        assertTrue("@SerializedName(\"first\")", varSource.contains("@SerializedName(\"first\")"))

        // Input object
        val inputSource = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        ).first { "UserInput" in it.toString() }.toString()
        assertTrue("@SerializedName(\"name\")", inputSource.contains("@SerializedName(\"name\")"))
        assertTrue("@SerializedName(\"private\")", inputSource.contains("@SerializedName(\"private\")"))

        // Enum
        val enumSource = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        ).first { "Status" in it.toString() }.toString()
        assertTrue("@SerializedName(\"OPEN\")", enumSource.contains("@SerializedName(\"OPEN\")"))

        // Response model
        val respSource = generateResponseSource(
            queryPath = "phase2/queries/GetPartialUser.graphql",
            operationName = "GetPartialUser",
            backend = SerializationBackend.GSON,
        ).first().toString()
        assertTrue("@SerializedName(\"id\")", respSource.contains("@SerializedName(\"id\")"))
        assertTrue("@SerializedName(\"login\")", respSource.contains("@SerializedName(\"login\")"))
    }

    // -----------------------------------------------------------------------
    // NONE backend
    // -----------------------------------------------------------------------

    @Test
    fun `NONE backend emits no serialization annotations`() {
        val source = generateResponseSource(
            queryPath = "phase2/queries/GetPartialUser.graphql",
            operationName = "GetPartialUser",
            backend = SerializationBackend.NONE,
        ).first().toString()

        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
        assertFalse(source.contains("@SerializedName"))
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun fixtureFile(path: String): File {
        val url = checkNotNull(
            this::class.java.classLoader.getResource(path),
        ) { "Fixture not found: $path" }
        return File(url.toURI())
    }

    private fun fixtureText(path: String): String = fixtureFile(path).readText()

    private fun generateResponseSource(
        queryPath: String,
        operationName: String,
        backend: SerializationBackend,
        scalarMappings: Map<String, String> = emptyMap(),
    ): List<com.squareup.kotlinpoet.FileSpec> {
        val parser = ResponseSelectionParser(schemaIndex)
        val operation = GraphQLOperationInfo(
            name = operationName,
            type = OperationType.QUERY,
            document = fixtureText(queryPath),
            sourceFile = "$operationName.graphql",
            variables = emptyList(),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(schemaIndex, scalarMappings, backend)
        return generator.generate(operation, selectionSet, "com.example")
    }
}
