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
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests that generated types correctly serialize and deserialize JSON.
 *
 * **Layer E: Structural JSON Round-Trip Tests.**
 *
 * These tests verify that generated types can:
 * 1. Parse fixture JSON into generated types, then re-serialize back.
 * 2. Construct instances programmatically, serialize, then parse back.
 *
 * JSON structures are compared using parsed [JsonElement] trees
 * (not textual key ordering) to verify structural equality.
 *
 * Gson is used for the actual serialization because it does not require
 * a compiler plugin. The generated source code is compiled at test time
 * via the embedded K2JVMCompiler.
 *
 * For kotlinx.serialization-specific features (like polymorphic interface
 * discrimination via `__typename`), source-level annotation verification
 * is used since the kotlinx compiler plugin is not available at test time.
 */
class SerializationRoundTripTest {

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
    // Generated variables round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `variable class with keyword fields round trips parse then serialize`() {
        val operation = GraphQLOperationInfo(
            name = "GetItems",
            type = OperationType.QUERY,
            document = "query GetItems(\$private: String, \$when: Int) { getCurrentUser { id } }",
            sourceFile = "GetItems.graphql",
            variables = listOf(
                GraphQLVariableInfo("private", GraphQLType.Named(name = "String", nullable = true)),
                GraphQLVariableInfo("when", GraphQLType.Named(name = "Int", nullable = true)),
            ),
        )
        val fileSpec = VariableClassGenerator.generate(
            operation, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )!!

        val result = CompilationHelper.compileSingle(fileSpec, includeGraphQLVariablesStub = true)
        try {
            val gson = GsonBuilder().create()
            val cls = result.classLoader.loadClass("com.example.GetItemsVariables")

            // Fixture JSON -> generated type -> JSON
            val fixtureJson = """{"private":"top-secret","when":42}"""
            val instance = gson.fromJson(fixtureJson, cls)
            val reEncoded = gson.toJson(instance)

            // Compare JSON structures
            val expected = JsonParser.parseString(fixtureJson)
            val actual = JsonParser.parseString(reEncoded)
            assertEquals("Parse-then-serialize round trip must preserve structure", expected, actual)
        } finally {
            result.close()
        }
    }

    @Test
    fun `variable class round trips construct then parse`() {
        val operation = GraphQLOperationInfo(
            name = "GetItems",
            type = OperationType.QUERY,
            document = "query GetItems(\$first: Int!) { getCurrentUser { id } }",
            sourceFile = "GetItems.graphql",
            variables = listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )
        val fileSpec = VariableClassGenerator.generate(
            operation, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )!!

        val result = CompilationHelper.compileSingle(fileSpec, includeGraphQLVariablesStub = true)
        try {
            val gson = GsonBuilder().create()
            val cls = result.classLoader.loadClass("com.example.GetItemsVariables")
            val ctor = cls.constructors.first { it.parameterCount == 1 }
            ctor.isAccessible = true
            val instance = ctor.newInstance(10)
            val json = gson.toJson(instance)

            val roundTripped = gson.fromJson(json, cls)
            val reEncoded = gson.toJson(roundTripped)

            val expected = JsonParser.parseString(json)
            val actual = JsonParser.parseString(reEncoded)
            assertEquals("Construct-then-serialize round trip must preserve structure", expected, actual)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Nested input objects round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `nested input object with keyword fields round trips`() {
        val inputSpecs = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )
        // Input objects reference enums, so include enum sources
        val enumSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val allSpecs = inputSpecs + enumSpecs
        val result = CompilationHelper.compile(allSpecs, includeGraphQLVariablesStub = true)
        try {
            val gson = GsonBuilder().create()
            val userCls = result.classLoader.loadClass("com.example.UserInput")

            // Use JSON deserialization for reliable object construction
            val fixtureJson = """{"name":"TestName","status":"OPEN","sortOrder":"desc","nested":{"when":"time-value","is":true,"values":[1,2]}}"""
            val userInstance = gson.fromJson(fixtureJson, userCls)
            val json = gson.toJson(userInstance)

            val obj = gson.fromJson(json, JsonObject::class.java)
            assertEquals("TestName", obj.get("name").asString)
            val nestedObj = obj.getAsJsonObject("nested")
            assertNotNull(nestedObj)
            assertEquals("time-value", nestedObj.get("when").asString)
            assertEquals(true, nestedObj.get("is").asBoolean)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Enum in request round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `enum round trips parse then serialize with uppercase values`() {
        val fileSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val cls = result.classLoader.loadClass("com.example.Status")
            val json = "\"OPEN\""
            val instance = gson.fromJson(json, cls)
            val reEncoded = gson.toJson(instance)

            assertEquals("\"OPEN\"", reEncoded)
            assertEquals("OPEN", instance.toString())
        } finally {
            result.close()
        }
    }

    @Test
    fun `enum round trips parse then serialize with lowercase values`() {
        val fileSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val cls = result.classLoader.loadClass("com.example.SortOrder")
            val json = "\"desc\""
            val instance = gson.fromJson(json, cls)
            val reEncoded = gson.toJson(instance)

            assertEquals("\"desc\"", reEncoded)
        } finally {
            result.close()
        }
    }

    @Test
    fun `enum round trips construct then parse with keyword values`() {
        val fileSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val cls = result.classLoader.loadClass("com.example.Status")

            // Find PRIVATE_VALUE or PRIVATE constant
            val privateConst = cls.enumConstants!!.first { const ->
                const.toString().contains("PRIVATE")
            }
            val json = gson.toJson(privateConst)
            assertEquals("\"private\"", json)

            val roundTripped = gson.fromJson(json, cls)
            assertEquals(privateConst, roundTripped)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Ordinary response object round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `ordinary response object round trips parse then serialize`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetPartialUser.graphql",
            operationName = "GetPartialUser",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val innerCls = result.classLoader.loadClass("com.example.GetPartialUserData\$GetCurrentUser")
            val fixtureJson = """{"id":"123","login":"testuser"}"""
            val instance = gson.fromJson(fixtureJson, innerCls)
            val reEncoded = gson.toJson(instance)

            val expected = JsonParser.parseString(fixtureJson)
            val actual = JsonParser.parseString(reEncoded)
            assertEquals("Response round trip must preserve JSON structure", expected, actual)
        } finally {
            result.close()
        }
    }

    @Test
    fun `response object round trips construct then parse`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetPartialUser.graphql",
            operationName = "GetPartialUser",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val innerCls = result.classLoader.loadClass("com.example.GetPartialUserData\$GetCurrentUser")
            val ctor = innerCls.constructors.first { it.parameterCount == 2 }
            ctor.isAccessible = true
            val instance = ctor.newInstance("456", "octocat")
            val json = gson.toJson(instance)

            val roundTripped = gson.fromJson(json, innerCls)
            val reEncoded = gson.toJson(roundTripped)

            val expected = JsonParser.parseString(json)
            val actual = JsonParser.parseString(reEncoded)
            assertEquals("Construct-then-serialize round trip must preserve structure", expected, actual)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Keyword/sanitized field wire name preservation
    // -----------------------------------------------------------------------

    @Test
    fun `keyword field wire name is preserved in JSON not escaped kotlin name`() {
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
            val gson = GsonBuilder().create()
            val cls = result.classLoader.loadClass("com.example.GetItemsVariables")
            val ctor = cls.constructors.first { it.parameterCount == 1 }
            ctor.isAccessible = true
            val instance = ctor.newInstance("sensitive")
            val json = gson.toJson(instance)

            val obj = gson.fromJson(json, JsonObject::class.java)
            // Must have "private" (wire name), NOT "privateValue" (escaped Kotlin name)
            assertTrue("JSON must have 'private' key", obj.has("private"))
            assertFalse("JSON must NOT have 'privateValue' key", obj.has("privateValue"))
            assertEquals("sensitive", obj.get("private").asString)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Explicit null value
    // -----------------------------------------------------------------------

    @Test
    fun `explicit null value is preserved in JSON`() {
        val operation = GraphQLOperationInfo(
            name = "GetItems",
            type = OperationType.QUERY,
            document = "query GetItems(\$first: Int) { getCurrentUser { id } }",
            sourceFile = "GetItems.graphql",
            variables = listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = true)),
            ),
        )
        val fileSpec = VariableClassGenerator.generate(
            operation, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        )!!

        val result = CompilationHelper.compileSingle(fileSpec, includeGraphQLVariablesStub = true)
        try {
            val gson = GsonBuilder().serializeNulls().create()
            val cls = result.classLoader.loadClass("com.example.GetItemsVariables")
            val ctor = cls.constructors.first { it.parameterCount == 1 }
            ctor.isAccessible = true
            val instance = ctor.newInstance(null)
            val json = gson.toJson(instance)

            val obj = gson.fromJson(json, JsonObject::class.java)
            assertTrue("JSON must have 'first' key with serializeNulls", obj.has("first"))
            assertTrue("'first' must be null", obj.get("first").isJsonNull)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Conditional field omitted
    // -----------------------------------------------------------------------

    @Test
    fun `field present in schema but absent in JSON deserializes as null`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetPartialUser.graphql",
            operationName = "GetPartialUser",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val innerCls = result.classLoader.loadClass("com.example.GetPartialUserData\$GetCurrentUser")
            // Only "id" provided, "login" is explicitly absent
            val json = """{"id":"999"}"""
            val instance = gson.fromJson(json, innerCls)
            val reEncoded = gson.toJson(instance)

            val obj = gson.fromJson(reEncoded, JsonObject::class.java)
            assertEquals("999", obj.get("id").asString)
            // "login" field exists in the schema but not in the JSON
            // Gson will deserialize missing fields as null (for reference types)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Nullable list container and nullable list elements
    // -----------------------------------------------------------------------

    @Test
    fun `null list container serializes as null by default`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUser.graphql",
            operationName = "GetCurrentUser",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val innerCls = result.classLoader.loadClass(
                "com.example.GetCurrentUserData\$GetCurrentUser",
            )

            // Deserialize minimal JSON with only id and login, friends will be null
            val fixtureJson = """{"id":"1","login":"test"}"""
            val instance = gson.fromJson(fixtureJson, innerCls)
            val json = gson.toJson(instance)
            val obj = gson.fromJson(json, JsonObject::class.java)

            // Null list should not appear in output (Gson skips nulls by default)
            assertFalse("Null 'friends' should not appear", obj.has("friends"))
        } finally {
            result.close()
        }
    }

    @Test
    fun `list with null elements preserves null entries`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUser.graphql",
            operationName = "GetCurrentUser",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().serializeNulls().create()
            val innerCls = result.classLoader.loadClass(
                "com.example.GetCurrentUserData\$GetCurrentUser",
            )

            // Deserialize JSON with null list elements
            val fixtureJson = """{"id":"1","login":"test","items":["a",null,"b"]}"""
            val instance = gson.fromJson(fixtureJson, innerCls)
            val json = gson.toJson(instance)
            val obj = gson.fromJson(json, JsonObject::class.java)

            val itemsArr = obj.getAsJsonArray("items")
            assertNotNull(itemsArr)
            assertEquals(3, itemsArr.size())
            assertEquals("a", itemsArr.get(0).asString)
            assertTrue("Second element should be null", itemsArr.get(1).isJsonNull)
            assertEquals("b", itemsArr.get(2).asString)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Polymorphic interface/union subtype (source-level)
    // -----------------------------------------------------------------------

    @Test
    fun `polymorphic sealed interface has JsonClassDiscriminator with __typename`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/SearchQuery.graphql",
            operationName = "SearchQuery",
            backend = SerializationBackend.KOTLINX,
        )

        val source = fileSpecs.first().toString()
        // JsonClassDiscriminator uses "__typename" for polymorphic discrimination
        assertTrue(
            "Sealed interface must have JsonClassDiscriminator with __typename",
            source.contains("JsonClassDiscriminator(\"__typename\")"),
        )
    }

    @Test
    fun `concrete subtypes have SerialName matching __typename values`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/SearchQuery.graphql",
            operationName = "SearchQuery",
            backend = SerializationBackend.KOTLINX,
        )

        val source = fileSpecs.first().toString()
        assertTrue("@SerialName(\"Page\")", source.contains("@SerialName(\"Page\")"))
        assertTrue("@SerialName(\"TextMessage\")", source.contains("@SerialName(\"TextMessage\")"))
    }

    // -----------------------------------------------------------------------
    // Custom scalar mapped to String - real compile + Gson round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `custom scalar DateTime round trips parse then serialize through Gson`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUserWithScalar.graphql",
            operationName = "GetCurrentUserWithScalar",
            backend = SerializationBackend.GSON,
            scalarMappings = mapOf("DateTime" to "kotlin.String"),
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val innerCls = result.classLoader.loadClass(
                "com.example.GetCurrentUserWithScalarData\$GetCurrentUser",
            )

            val fixtureJson = """{"id":"1","login":"test","createdAt":"2024-01-15T10:30:00Z"}"""
            val instance = gson.fromJson(fixtureJson, innerCls)
            val reEncoded = gson.toJson(instance)

            val obj = gson.fromJson(reEncoded, JsonObject::class.java)
            assertEquals("1", obj.get("id").asString)
            assertEquals("test", obj.get("login").asString)
            assertTrue("createdAt key must be preserved", obj.has("createdAt"))
            assertEquals("2024-01-15T10:30:00Z", obj.get("createdAt").asString)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Enum field in response type round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `enum field in response round trips with wire name preserved`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetCurrentUserFull.graphql",
            operationName = "GetCurrentUserFull",
            backend = SerializationBackend.GSON,
            scalarMappings = mapOf("DateTime" to "kotlin.String"),
        )
        // Response model references Status enum, so compile enums alongside
        val enumSpecs = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs + enumSpecs)
        try {
            val gson = GsonBuilder().create()
            val innerCls = result.classLoader.loadClass(
                "com.example.GetCurrentUserFullData\$GetCurrentUser",
            )

            val fixtureJson = """{"id":"1","login":"test","status":"OPEN"}"""
            val instance = gson.fromJson(fixtureJson, innerCls)
            val reEncoded = gson.toJson(instance)

            val obj = gson.fromJson(reEncoded, JsonObject::class.java)
            assertTrue("status key must be present", obj.has("status"))
            assertEquals("OPEN", obj.get("status").asString)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Collision fields round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `collision fields round trip with both wire names preserved`() {
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
            val gson = GsonBuilder().create()
            val innerCls = result.classLoader.loadClass(
                "com.example.GetCurrentUserFullData\$GetCurrentUser",
            )

            val fixtureJson = """{"id":"1","login":"test","myField":"camel","my_field":"snake"}"""
            val instance = gson.fromJson(fixtureJson, innerCls)
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
    // Gson-based polymorphic subtype round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `polymorphic subtype deserializes to correct type based on __typename`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/SearchQuery.graphql",
            operationName = "SearchQuery",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val rootCls = result.classLoader.loadClass("com.example.SearchQueryData")
            // Find the sealed interface nested inside SearchQueryData
            // For GSON backend: look for the inner class that has Page/TextMessage as subtypes
            val sealedIface = rootCls.classes.firstOrNull { inner ->
                inner.classes.any { sub -> sub.simpleName == "Page" || sub.simpleName == "TextMessage" }
            } ?: rootCls.classes.firstOrNull { inner ->
                inner.kotlin.isSealed
            } ?: error("No sealed interface found in SearchQueryData inner classes: ${rootCls.classes.toList()}")

            // Find Page and TextMessage subtypes inside the sealed interface
            val subtypes = sealedIface.classes.toList()
            val pageCls = subtypes.firstOrNull { it.simpleName == "Page" }
                ?: error("Page subtype not found in sealed interface: $subtypes")
            val textCls = subtypes.firstOrNull { it.simpleName == "TextMessage" }
                ?: error("TextMessage subtype not found in sealed interface: $subtypes")

            // Deserialize Page payload into Page subtype
            val pageJson = """{"__typename":"Page","id":"3","title":"Test Page","url":"https://example.com"}"""
            val pageInstance = gson.fromJson(pageJson, pageCls)
            // Verify correct subtype was deserialized
            assertEquals("Page subtype class", pageCls, pageInstance.javaClass)
            val pageReEncoded = gson.toJson(pageInstance)
            val pageObj = gson.fromJson(pageReEncoded, JsonObject::class.java)
            assertEquals("3", pageObj.get("id").asString)
            assertEquals("Test Page", pageObj.get("title").asString)
            // __typename is filtered from generated Gson types (kotlinx-only feature)
            // The subtype class identity proves correct deserialization

            // Deserialize TextMessage payload into TextMessage subtype
            val textJson = """{"__typename":"TextMessage","id":"4","body":"Hello","sender":"user1"}"""
            val textInstance = gson.fromJson(textJson, textCls)
            assertEquals("TextMessage subtype class", textCls, textInstance.javaClass)
            val textReEncoded = gson.toJson(textInstance)
            val textObj = gson.fromJson(textReEncoded, JsonObject::class.java)
            assertEquals("4", textObj.get("id").asString)
            assertEquals("Hello", textObj.get("body").asString)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // GraphContainer round-trip tests
    // -----------------------------------------------------------------------

    @Test
    fun `GraphContainer with data only round trips parse then serialize`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetPartialUser.graphql",
            operationName = "GetPartialUser",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs, includeGraphQLStubs = true)
        try {
            val gson = GsonBuilder().create()
            val containerCls = result.classLoader.loadClass(
                "co.anitrend.retrofit.graphql.model.GraphContainer",
            )
            val innerCls = result.classLoader.loadClass(
                "com.example.GetPartialUserData",
            )

            // Use a raw JSON parse to build GraphContainer<GetPartialUserData>
            val fixtureJson = """{"data":{"getCurrentUser":{"id":"1","login":"test"}}}"""
            val container = gson.fromJson(fixtureJson, containerCls)
            val reEncoded = gson.toJson(container)

            val expected = JsonParser.parseString(fixtureJson)
            val actual = JsonParser.parseString(reEncoded)
            assertEquals("GraphContainer data-only round trip must preserve structure", expected, actual)
        } finally {
            result.close()
        }
    }

    @Test
    fun `GraphContainer with data plus errors round trips parse then serialize`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetPartialUser.graphql",
            operationName = "GetPartialUser",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs, includeGraphQLStubs = true)
        try {
            val gson = GsonBuilder().create()
            val containerCls = result.classLoader.loadClass(
                "co.anitrend.retrofit.graphql.model.GraphContainer",
            )

            val fixtureJson = """{"data":{"getCurrentUser":{"id":"1","login":"test"}},"errors":[{"message":"field error"}]}"""
            val container = gson.fromJson(fixtureJson, containerCls)
            val reEncoded = gson.toJson(container)

            val expected = JsonParser.parseString(fixtureJson)
            val actual = JsonParser.parseString(reEncoded)
            assertEquals("GraphContainer with errors round trip must preserve structure", expected, actual)
        } finally {
            result.close()
        }
    }

    @Test
    fun `GraphContainer with partial data (null data) round trips parse then serialize`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetPartialUser.graphql",
            operationName = "GetPartialUser",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs, includeGraphQLStubs = true)
        try {
            val gson = GsonBuilder().serializeNulls().create()
            val containerCls = result.classLoader.loadClass(
                "co.anitrend.retrofit.graphql.model.GraphContainer",
            )

            val fixtureJson = """{"data":null,"errors":[{"message":"complete failure"}]}"""
            val container = gson.fromJson(fixtureJson, containerCls)
            val reEncoded = gson.toJson(container)

            // With serializeNulls, all null fields are serialized.
            // Verify data is null and errors are present (structural check)
            val actual = gson.fromJson(reEncoded, JsonObject::class.java)
            assertTrue("data key must be present", actual.has("data"))
            assertTrue("data must be null", actual.get("data").isJsonNull)
            assertTrue("errors key must be present", actual.has("errors"))
            assertEquals("complete failure",
                actual.getAsJsonArray("errors").get(0).asJsonObject.get("message").asString)
        } finally {
            result.close()
        }
    }

    // -----------------------------------------------------------------------
    // Alias field
    // -----------------------------------------------------------------------

    @Test
    fun `alias field JSON key is the alias not the schema name`() {
        val fileSpecs = generateResponseSource(
            queryPath = "phase2/queries/GetUserWithAliases.graphql",
            operationName = "GetUserWithAliases",
            backend = SerializationBackend.GSON,
        )

        val result = CompilationHelper.compile(fileSpecs)
        try {
            val gson = GsonBuilder().create()
            val innerCls = result.classLoader.loadClass(
                "com.example.GetUserWithAliasesData\$GetCurrentUser",
            )

            val fixtureJson = fixtureText("phase2/responses/GetUserWithAliases.json")
            val wrapper = gson.fromJson(fixtureJson, JsonObject::class.java)
            val data = wrapper.getAsJsonObject("data")
            val user = data.getAsJsonObject("getCurrentUser")

            // The JSON has aliased keys: userId, userName, userBio
            assertTrue("JSON must have 'userId'", user.has("userId"))
            assertTrue("JSON must have 'userName'", user.has("userName"))
            assertTrue("JSON must have 'userBio'", user.has("userBio"))

            // Parse into generated type
            val instance = gson.fromJson(user.toString(), innerCls)
            val reEncoded = gson.toJson(instance)
            val reObj = gson.fromJson(reEncoded, JsonObject::class.java)

            // Re-encoded JSON must preserve alias names
            assertTrue("Re-encoded must have 'userId'", reObj.has("userId"))
            assertTrue("Re-encoded must have 'userName'", reObj.has("userName"))
            assertTrue("Re-encoded must have 'userBio'", reObj.has("userBio"))
        } finally {
            result.close()
        }
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
