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
import co.anitrend.retrofit.graphql.codegen.generate.helper.CompilationHelper
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import co.anitrend.retrofit.graphql.codegen.parser.ResponseSelectionParser
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.reflect.Field

/**
 * Tests that Gson serialization contracts are correct at runtime.
 *
 * **Layer F: Gson Wire-Contract Tests.**
 *
 * These tests compile generated source code with the GSON backend and
 * then use Gson at runtime to verify:
 * - `@SerializedName` values produce correct JSON keys.
 * - Encode/decode preserves GraphQL wire names.
 * - `FieldNamingPolicy` CANNOT override explicit `@SerializedName` values.
 * - Keyword fields, aliases, enums, and collisions are handled correctly.
 *
 * Gson does not require a compiler plugin, so these tests can compile
 * and load generated code directly at test time.
 */
class GsonWireContractTest {

    private lateinit var schemaIndex: SchemaIndex
    private val gson: Gson = GsonBuilder().create()

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
    // Gson: Keyword field encode/decode
    // -----------------------------------------------------------------------

    @Test
    fun `keyword fields encode with correct wire names using Gson`() {
        val inputSpecs = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )
        val enumSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val allSpecs = inputSpecs + enumSpecs
        val result = CompilationHelper.compile(allSpecs, includeGraphQLVariablesStub = true)
        try {
            val cls = result.classLoader.loadClass("com.example.NestedInput")
            // Create instance using constructor: when="test", is=true, values=[1,2,3]
            val ctor = cls.constructors.first { it.parameterCount == 3 }
            ctor.isAccessible = true
            val instance = ctor.newInstance("test", true, listOf(1, 2, 3))
            val json = gson.toJson(instance)

            val obj = gson.fromJson(json, JsonObject::class.java)
            // Wire names must match GraphQL names, NOT escaped Kotlin names
            assertTrue("JSON must have 'when' key", obj.has("when"))
            assertTrue("JSON must have 'is' key", obj.has("is"))
            assertTrue("JSON must have 'values' key", obj.has("values"))
            assertEquals("test", obj.get("when").asString)
            assertEquals(true, obj.get("is").asBoolean)
        } finally {
            result.close()
        }
    }

    @Test
    fun `keyword fields decode from correct wire names using Gson`() {
        val inputSpecs = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )
        val enumSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val allSpecs = inputSpecs + enumSpecs
        val result = CompilationHelper.compile(allSpecs, includeGraphQLVariablesStub = true)
        try {
            val cls = result.classLoader.loadClass("com.example.NestedInput")
            val json = """{"when":"test-time","is":true,"values":[4,5]}"""
            val instance = gson.fromJson(json, cls)

            // Verify values were decoded correctly
            val whenField = findField(cls, "whenValue") ?: findField(cls, "when")
                ?: error("Neither 'whenValue' nor 'when' field found in NestedInput")
            whenField.isAccessible = true
            assertEquals("test-time", whenField.get(instance))

            val isField = findField(cls, "isValue") ?: findField(cls, "is")
                ?: error("Neither 'isValue' nor 'is' field found in NestedInput")
            isField.isAccessible = true
            assertEquals(true, isField.get(instance))
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Gson: Enum encode/decode
    // -----------------------------------------------------------------------

    @Test
    fun `enum encodes with correct wire names lowercase values`() {
        val fileSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val cls = result.classLoader.loadClass("com.example.SortOrder")
            val ascValue = cls.enumConstants!!.first { it.toString() == "ASC" }
            val json = gson.toJson(ascValue)

            // Lowercase wire name must be preserved
            assertEquals("\"asc\"", json)

            val decoded = gson.fromJson("\"desc\"", cls)
            assertEquals("DESC", decoded.toString())
        } finally {
            result.close()
        }
    }

    @Test
    fun `enum keyword values encode with correct wire names`() {
        val fileSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val cls = result.classLoader.loadClass("com.example.Status")
            val privateValue = cls.enumConstants!!.first { it.toString().contains("PRIVATE") }
            val json = gson.toJson(privateValue)

            // Keyword enum value: wire name is the GraphQL value, not escaped
            assertEquals("\"private\"", json)

            val decoded = gson.fromJson("\"private\"", cls)
            assertTrue(decoded.toString().contains("PRIVATE"))
        } finally {
            result.close()
        }
    }

    @Test
    fun `enum round trip with all values`() {
        val fileSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val cls = result.classLoader.loadClass("com.example.MediaType")
            val anime = cls.enumConstants!!.first { it.toString() == "ANIME" }
            val manga = cls.enumConstants!!.first { it.toString() == "MANGA" }
            val lightNovel = cls.enumConstants!!.first { it.toString().contains("LIGHTNOVEL") }

            assertEquals("\"ANIME\"", gson.toJson(anime))
            assertEquals("\"MANGA\"", gson.toJson(manga))
            assertEquals("\"lightNovel\"", gson.toJson(lightNovel))

            assertEquals("ANIME", gson.fromJson("\"ANIME\"", cls).toString())
            assertEquals("MANGA", gson.fromJson("\"MANGA\"", cls).toString())
            assertEquals(lightNovel.toString(), gson.fromJson("\"lightNovel\"", cls).toString())
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Gson: Input object round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `input object with keyword fields round trips through Gson`() {
        val inputSpecs = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )
        val enumSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val allSpecs = inputSpecs + enumSpecs
        val result = CompilationHelper.compile(allSpecs, includeGraphQLVariablesStub = true)
        try {
            val cls = result.classLoader.loadClass("com.example.UserInput")

            // Deserialize from JSON fixture instead of manually constructing
            val fixtureJson = """{"name":"TestUser","private":true,"object":"obj-value"}"""
            val instance = gson.fromJson(fixtureJson, cls)
            val json = gson.toJson(instance)

            val obj = gson.fromJson(json, JsonObject::class.java)
            assertEquals("TestUser", obj.get("name").asString)
            assertTrue(obj.has("private"))
            assertTrue(obj.has("object"))
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Gson: Variable class round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `variable class with keyword fields round trips through Gson`() {
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
        val fileSpec = VariableClassGenerator.generate(
            operation, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )!!

        val result = CompilationHelper.compileSingle(fileSpec, includeGraphQLVariablesStub = true)
        try {
            val cls = result.classLoader.loadClass("com.example.GetItemsVariables")
            val ctor = cls.constructors.first { it.parameterCount == 3 }
            ctor.isAccessible = true
            val instance = ctor.newInstance("secret", true, 42)
            val json = gson.toJson(instance)

            val obj = gson.fromJson(json, JsonObject::class.java)
            assertTrue("JSON must have 'private' key", obj.has("private"))
            assertTrue("JSON must have 'object' key", obj.has("object"))
            assertTrue("JSON must have 'when' key", obj.has("when"))
            assertEquals("secret", obj.get("private").asString)
            assertEquals(true, obj.get("object").asBoolean)
            assertEquals(42, obj.get("when").asInt)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Gson: FieldNamingPolicy cannot override @SerializedName
    // -----------------------------------------------------------------------

    @Test
    fun `FieldNamingPolicy lower case with underscores cannot change enum wire name`() {
        val fileSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val cls = result.classLoader.loadClass("com.example.MediaType")
            val lightNovel = cls.enumConstants!!.first { it.toString().contains("LIGHTNOVEL") }

            // Even with a naming policy, @SerializedName takes precedence
            val gsonWithPolicy = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()

            val json = gsonWithPolicy.toJson(lightNovel)
            // lightNovel wire name is "lightNovel" - the naming policy must NOT change this
            assertEquals("\"lightNovel\"", json)
        } finally {
            result.close()
        }
    }

    @Test
    fun `FieldNamingPolicy upper camel case cannot change input field wire name`() {
        val inputSpecs = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )
        val enumSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val allSpecs = inputSpecs + enumSpecs
        val result = CompilationHelper.compile(allSpecs, includeGraphQLVariablesStub = true)
        try {
            val cls = result.classLoader.loadClass("com.example.UserInput")

            val fixtureJson = """{"name":"TestName"}"""
            val instance = gson.fromJson(fixtureJson, cls)

            val gsonWithPolicy = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .create()

            val json = gsonWithPolicy.toJson(instance)
            val obj = gson.fromJson(json, JsonObject::class.java)

            // "name" must remain "name" (not "Name") because @SerializedName overrides
            assertTrue("JSON must have 'name' key (not 'Name')", obj.has("name"))
            assertEquals("TestName", obj.get("name").asString)
        } finally {
            result.close()
        }
    }

    @Test
    fun `FieldNamingPolicy cannot change keyword wire name on variables`() {
        val operation = GraphQLOperationInfo(
            name = "GetItems",
            type = OperationType.QUERY,
            document = "query GetItems(\$private: String) { getCurrentUser { id } }",
            sourceFile = "GetItems.graphql",
            variables = listOf(
                GraphQLVariableInfo("private", GraphQLType.Named(name = "String", nullable = true)),
            ),
        )
        val fileSpec = VariableClassGenerator.generate(
            operation, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )!!

        val result = CompilationHelper.compileSingle(fileSpec, includeGraphQLVariablesStub = true)
        try {
            val cls = result.classLoader.loadClass("com.example.GetItemsVariables")
            val ctor = cls.constructors.first { it.parameterCount == 1 }
            ctor.isAccessible = true
            val instance = ctor.newInstance("top-secret")

            val gsonWithPolicy = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()

            val json = gsonWithPolicy.toJson(instance)
            val obj = gson.fromJson(json, JsonObject::class.java)

            // Must use "private" (wire name), not "private_value" which the policy would produce
            assertTrue("JSON must have 'private' key", obj.has("private"))
            assertTrue("JSON must NOT have 'private_value' key", !obj.has("private_value"))
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Gson: response model round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `simple response model round trips through Gson`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetPartialUser.graphql",
            operationName = "GetPartialUser",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val innerCls = result.classLoader.loadClass("com.example.GetPartialUserData\$GetCurrentUser")

            val json = """{"id":"123","login":"testuser"}"""
            val instance = gson.fromJson(json, innerCls)

            // Re-encode
            val reEncoded = gson.toJson(instance)
            val reObj = gson.fromJson(reEncoded, JsonObject::class.java)
            assertEquals("123", reObj.get("id").asString)
            assertEquals("testuser", reObj.get("login").asString)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Gson: alias encode/decode
    // -----------------------------------------------------------------------

    @Test
    fun `alias fields round trip through Gson with alias keys preserved`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetUserWithAliases.graphql",
            operationName = "GetUserWithAliases",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val innerCls = result.classLoader.loadClass(
                "com.example.GetUserWithAliasesData\$GetCurrentUser",
            )

            val json = """{"id":"1","userId":"1","userName":"x","userBio":"y"}"""
            val instance = gson.fromJson(json, innerCls)
            val reEncoded = gson.toJson(instance)
            val obj = gson.fromJson(reEncoded, JsonObject::class.java)

            // Alias keys must be preserved, NOT the original schema names
            assertTrue("Re-encoded must have 'userId'", obj.has("userId"))
            assertTrue("Re-encoded must have 'userName'", obj.has("userName"))
            assertTrue("Re-encoded must have 'userBio'", obj.has("userBio"))
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Gson: collision fields encode/decode
    // -----------------------------------------------------------------------

    @Test
    fun `collision fields round trip through Gson with distinct wire names`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUserFull.graphql",
            operationName = "GetCurrentUserFull",
            backend = SerializationBackend.GSON,
            scalarMappings = mapOf("DateTime" to "kotlin.String"),
        )
        val enumSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs + enumSpecs)
        try {
            val innerCls = result.classLoader.loadClass(
                "com.example.GetCurrentUserFullData\$GetCurrentUser",
            )

            val json = """{"id":"1","login":"test","myField":"camel","my_field":"snake"}"""
            val instance = gson.fromJson(json, innerCls)
            val reEncoded = gson.toJson(instance)
            val obj = gson.fromJson(reEncoded, JsonObject::class.java)

            assertTrue("myField key must be preserved", obj.has("myField"))
            assertTrue("my_field key must be preserved", obj.has("my_field"))
            assertEquals("camel", obj.get("myField").asString)
            assertEquals("snake", obj.get("my_field").asString)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun findField(cls: Class<*>, name: String): Field? {
        return cls.declaredFields.firstOrNull { it.name == name }
    }

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
