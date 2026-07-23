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
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.parser.ResponseSelectionParser
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ResponseModelGeneratorTest {

    private lateinit var githubIndex: SchemaIndex
    private lateinit var anilistIndex: SchemaIndex
    private lateinit var parser: ResponseSelectionParser
    private lateinit var generator: ResponseModelGenerator

    @Before
    fun setUp() {
        // Parse github-simple schema
        val githubSchemaFile = fixtureFile(
            "fixtures/simple/schemas/github-simple.graphqls",
        )
        val githubResult = SchemaParser().parseWithRootTypes(githubSchemaFile)
        githubIndex = SchemaIndex.from(
            types = githubResult.types,
            queryTypeName = githubResult.queryTypeName,
            mutationTypeName = githubResult.mutationTypeName,
            subscriptionTypeName = githubResult.subscriptionTypeName,
        )

        // Parse anilist schema
        val anilistSchemaFile = fixtureFile("fixtures/schemas/anilist.graphqls")
        val anilistResult = SchemaParser().parseWithRootTypes(anilistSchemaFile)
        anilistIndex = SchemaIndex.from(
            types = anilistResult.types,
            queryTypeName = anilistResult.queryTypeName,
            mutationTypeName = anilistResult.mutationTypeName,
            subscriptionTypeName = anilistResult.subscriptionTypeName,
        )

        parser = ResponseSelectionParser(githubIndex)
        generator = ResponseModelGenerator(githubIndex)
    }

    // --- Simple query model ---

    @Test
    fun `generate simple query model with nested User type`() {
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "GetUser",
            type = OperationType.QUERY,
            document = fixtureText("fixtures/simple/queries/GetUser.graphql"),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        assertEquals(1, fileSpecs.size)
        val fileSpec = fileSpecs.first()

        // Find the GetUserData class
        val dataClass = fileSpec.members
            .filterIsInstance<TypeSpec>()
            .firstOrNull { it.name == "GetUserData" }
        assertNotNull("Should contain GetUserData class", dataClass)

        // Check it's a data class
        assertTrue(
            "GetUserData should be a data class",
            dataClass!!.modifiers.contains(KModifier.DATA),
        )

        // Check @Serializable annotation
        assertTrue(
            "GetUserData should have @Serializable annotation",
            dataClass.annotations.any {
                it is AnnotationSpec &&
                    it.typeName == ClassName("kotlinx.serialization", "Serializable")
            },
        )

        // Check viewer property
        val viewerProp = dataClass.propertySpecs.firstOrNull { it.name == "viewer" }
        assertNotNull("Should have viewer property", viewerProp)
        assertTrue(
            "viewer should be public",
            viewerProp!!.modifiers.contains(KModifier.PUBLIC),
        )

        // Check nested Viewer class exists (path-based name instead of "User")
        val viewerClass = dataClass.typeSpecs.firstOrNull { it.name == "Viewer" }
        assertNotNull("Should contain nested Viewer class", viewerClass)
        assertTrue(
            "Viewer should be a data class",
            viewerClass!!.modifiers.contains(KModifier.DATA),
        )
        assertTrue(
            "Viewer should have @Serializable annotation",
            viewerClass.annotations.any {
                it is AnnotationSpec &&
                    it.typeName == ClassName("kotlinx.serialization", "Serializable")
            },
        )

        // Check Viewer has expected fields
        val viewerFieldNames = viewerClass.propertySpecs.map { it.name }.toSet()
        assertTrue("Viewer should have id field", "id" in viewerFieldNames)
        assertTrue("Viewer should have login field", "login" in viewerFieldNames)
        assertTrue("Viewer should have name field", "name" in viewerFieldNames)
        assertTrue("Viewer should have bio field", "bio" in viewerFieldNames)
    }

    // --- Mutation model ---

    @Test
    fun `generate mutation model with nested UpdateBioPayload and User`() {
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "UpdateBio",
            type = OperationType.MUTATION,
            document = fixtureText("fixtures/simple/mutations/UpdateBio.graphql"),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        assertEquals(1, fileSpecs.size)
        val fileSpec = fileSpecs.first()

        val dataClass = fileSpec.members
            .filterIsInstance<TypeSpec>()
            .firstOrNull { it.name == "UpdateBioData" }
        assertNotNull("Should contain UpdateBioData class", dataClass)

        // Check updateBio property
        val updateBioProp = dataClass!!.propertySpecs.firstOrNull { it.name == "updateBio" }
        assertNotNull("Should have updateBio property", updateBioProp)

        // Check nested UpdateBio class (path-based name)
        val payloadClass = dataClass.typeSpecs.firstOrNull { it.name == "UpdateBio" }
        assertNotNull("Should contain nested UpdateBio class", payloadClass)

        // Check UpdateBio has user property
        val userProp = payloadClass!!.propertySpecs.firstOrNull { it.name == "user" }
        assertNotNull("UpdateBio should have user property", userProp)

        // Check nested UpdateBioUser class (path-based name)
        val userClass = dataClass.typeSpecs.firstOrNull { it.name == "UpdateBioUser" }
        assertNotNull("Should contain nested UpdateBioUser class", userClass)
        val userFieldNames = userClass!!.propertySpecs.map { it.name }.toSet()
        assertTrue("UpdateBioUser should have id field", "id" in userFieldNames)
        assertTrue("UpdateBioUser should have bio field", "bio" in userFieldNames)
    }

    // --- @SerialName for aliases ---

    @Test
    fun `serialName annotation for aliased fields`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val operation = buildOperation(
            name = "AliasedFields",
            type = OperationType.QUERY,
            document = fixtureText("fixtures/advanced/aliases/AliasedFields.graphql"),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(anilistIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        assertEquals(1, fileSpecs.size)
        val fileSpec = fileSpecs.first()

        val dataClass = fileSpec.members
            .filterIsInstance<TypeSpec>()
            .firstOrNull { it.name == "AliasedFieldsData" }
        assertNotNull("Should contain AliasedFieldsData class", dataClass)

        // Find the Media nested class
        val mediaClass = dataClass!!.typeSpecs.firstOrNull { it.name == "Media" }
        assertNotNull("Should contain nested Media class", mediaClass)

        // Check for englishTitle alias with @SerialName
        val englishTitleProp = mediaClass!!.propertySpecs.firstOrNull { it.name == "englishTitle" }
        assertNotNull("Should have englishTitle property", englishTitleProp)
        val hasEnglishSerialName = englishTitleProp!!.annotations.any {
            it is AnnotationSpec &&
                it.typeName == ClassName("kotlinx.serialization", "SerialName") &&
                it.members.any { m -> m.toString().contains("\"englishTitle\"") }
        }
        assertTrue("englishTitle should have @SerialName(\"englishTitle\")", hasEnglishSerialName)

        // Check for nativeTitle alias with @SerialName
        val nativeTitleProp = mediaClass.propertySpecs.firstOrNull { it.name == "nativeTitle" }
        assertNotNull("Should have nativeTitle property", nativeTitleProp)
        val hasNativeSerialName = nativeTitleProp!!.annotations.any {
            it is AnnotationSpec &&
                it.typeName == ClassName("kotlinx.serialization", "SerialName") &&
                it.members.any { m -> m.toString().contains("\"nativeTitle\"") }
        }
        assertTrue("nativeTitle should have @SerialName(\"nativeTitle\")", hasNativeSerialName)
    }

    // --- Nested object types ---

    @Test
    fun `nested object types are generated as inner data classes with path-based names`() {
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "GetUser",
            type = OperationType.QUERY,
            document = fixtureText("fixtures/simple/queries/GetUser.graphql"),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "GetUserData" }

        // Viewer is a nested class with its own fields (path-based name)
        val viewerClass = dataClass.typeSpecs.firstOrNull { it.name == "Viewer" }
        assertNotNull("Viewer should be a nested class", viewerClass)
        assertEquals("Viewer", viewerClass!!.name)
        assertTrue(
            "Viewer should be a data class",
            viewerClass.modifiers.contains(KModifier.DATA),
        )

        // Viewer should have the scalar fields from the query
        val fieldNames = viewerClass.propertySpecs.map { it.name }
        assertTrue(fieldNames.contains("bio"))
        assertTrue(fieldNames.contains("id"))
        assertTrue(fieldNames.contains("login"))
        assertTrue(fieldNames.contains("name"))
    }

    // --- List types ---

    @Test
    fun `list types generate List of ElementType properties`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val document = """query MediaList { Page { media { id } } }"""
        val operation = buildOperation(
            name = "MediaList",
            type = OperationType.QUERY,
            document = document,
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(anilistIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "MediaListData" }

        // Page should have a media property
        val pageClass = dataClass.typeSpecs.firstOrNull { it.name == "Page" }
        assertNotNull("Should have nested Page class", pageClass)

        val mediaProp = pageClass!!.propertySpecs.firstOrNull { it.name == "media" }
        assertNotNull("Page should have media property", mediaProp)

        // The media property type should be a parameterized List type
        val mediaType = mediaProp!!.type
        assertTrue(
            "media type should be parameterized: $mediaType",
            mediaType.toString().contains("List"),
        )
    }

    // --- Deterministic output ---

    @Test
    fun `generation is deterministic for same input`() {
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "GetUser",
            type = OperationType.QUERY,
            document = fixtureText("fixtures/simple/queries/GetUser.graphql"),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val result1 = generator.generate(operation, selectionSet, "com.example")
        val result2 = generator.generate(operation, selectionSet, "com.example")

        assertEquals(
            "File count should be identical",
            result1.size,
            result2.size,
        )

        assertEquals(
            "Generated output should be identical",
            result1.first().toString(),
            result2.first().toString(),
        )
    }

    // --- Collision-safe naming ---

    @Test
    fun `same object type referenced from multiple paths generates separate per-path classes`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Query that references User at two different response-name paths.
        // With path-based identity, each unique path produces its own class.
        val document = """
            query DuplicateUser {
              viewer {
                id
                login
              }
              v: viewer {
                name
                bio
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "DuplicateUser",
            type = OperationType.QUERY,
            document = document,
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "DuplicateUserData" }

        // Two separate classes: Viewer (with id, login) and V (with name, bio)
        val viewerClass = dataClass.typeSpecs.find { it.name == "Viewer" }
        assertNotNull("Should contain Viewer class", viewerClass)
        val viewerFieldNames = viewerClass!!.propertySpecs.map { it.name }
        assertTrue("Viewer should have id", "id" in viewerFieldNames)
        assertTrue("Viewer should have login", "login" in viewerFieldNames)

        val vClass = dataClass.typeSpecs.find { it.name == "V" }
        assertNotNull("Should contain V class", vClass)
        val vFieldNames = vClass!!.propertySpecs.map { it.name }
        assertTrue("V should have name", "name" in vFieldNames)
        assertTrue("V should have bio", "bio" in vFieldNames)

        // No merged "User" class
        val userClasses = dataClass.typeSpecs.filter { it.name == "User" }
        assertEquals("User class should NOT exist (path-based identity)", 0, userClasses.size)
    }

    // --- Properties sorted by responseName ---

    @Test
    fun `properties are sorted by responseName within each class`() {
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "GetUser",
            type = OperationType.QUERY,
            document = fixtureText("fixtures/simple/queries/GetUser.graphql"),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "GetUserData" }

        // Properties on root class should be sorted
        val rootPropNames = dataClass.propertySpecs.map { it.name }
        assertEquals(rootPropNames.sorted(), rootPropNames)

        // Properties on nested Viewer class should be sorted (path-based name)
        val viewerClass = dataClass.typeSpecs.first { it.name == "Viewer" }
        val viewerPropNames = viewerClass.propertySpecs.map { it.name }
        assertEquals(viewerPropNames.sorted(), viewerPropNames)
    }

    // --- Custom dataClassName override ---

    @Test
    fun `custom dataClassName is respected`() {
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "GetUser",
            type = OperationType.QUERY,
            document = fixtureText("fixtures/simple/queries/GetUser.graphql"),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(
            operation = operation,
            selectionSet = selectionSet,
            packageName = "com.example",
            dataClassName = "CustomName",
        )

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .firstOrNull { it.name == "CustomName" }
        assertNotNull("Should use custom class name 'CustomName'", dataClass)
    }

    // --- GraphQLOperationInfo parameter is accessible ---

    @Test
    fun `operation name is used in default data class name`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query MyQuery {
              viewer {
                id
                login
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "MyQuery",
            type = OperationType.QUERY,
            document = document,
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .firstOrNull { it.name == "MyQueryData" }
        assertNotNull(
            "Should default to MyQueryData based on operation name",
            dataClass,
        )
    }

    // --- Phase 4: Conditional Presence ---

    @Test
    fun `conditional field gets nullable type with null default in primary constructor`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Create a query with a conditional @include directive
        val document =
            """
            query CondQuery {
              viewer {
                id
                name @include(if: true)
                login
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "CondQuery",
            type = OperationType.QUERY,
            document = document,
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "CondQueryData" }

        val viewerType = dataClass.typeSpecs.find { it.name == "Viewer" }!!
        val nameProp = viewerType.propertySpecs.find { it.name == "name" }!!
        val loginProp = viewerType.propertySpecs.find { it.name == "login" }!!

        // Conditional field should be nullable
        assertTrue("Conditional name should be nullable", nameProp.type.isNullable)

        // The constructor parameter for 'name' should have a null default
        val primaryCtor = viewerType.primaryConstructor!!
        val nameParam = primaryCtor.parameters.find { it.name == "name" }!!
        assertNotNull(
            "Constructor param for 'name' should have default value",
            nameParam.defaultValue,
        )
        assertTrue(
            "Constructor param default should contain 'null'",
            nameParam.defaultValue.toString().contains("null"),
        )

        // Non-conditional login param should NOT have a default
        val loginParam = primaryCtor.parameters.find { it.name == "login" }!!
        assertNull(
            "Constructor param for 'login' should NOT have default value",
            loginParam.defaultValue,
        )

        // Non-conditional field should NOT be nullable (String! in schema)
        assertTrue("Login should be non-null from schema", !loginProp.type.isNullable)
    }

    // --- Phase 4: Polymorphism ---

    @Test
    fun `generates sealed interface for interface fields with hierarchy-local discriminator`() {
        val parser = ResponseSelectionParser(githubIndex)
        // github-simple has Node interface with User implementing it
        val document =
            """
            query NodeQuery {
              node(id: "123") {
                id
                ... on User {
                  login
                  name
                }
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "NodeQuery",
            type = OperationType.QUERY,
            document = document,
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "NodeQueryData" }

        // Should have a sealed interface for Node
        val nodeInterface = dataClass.typeSpecs.find { it.name == "Node" }
        assertNotNull("Should have sealed interface for Node", nodeInterface)
        assertTrue(
            "Node should be sealed",
            nodeInterface!!.modifiers.contains(KModifier.SEALED),
        )
        assertTrue(
            "Node should be an interface",
            nodeInterface.kind == TypeSpec.Kind.INTERFACE,
        )

        // Should have @JsonClassDiscriminator("__typename") annotation
        val hasDiscriminator = nodeInterface.annotations.any { ann ->
            ann is AnnotationSpec &&
                ann.typeName.toString() == "kotlinx.serialization.json.JsonClassDiscriminator"
        }
        assertTrue("Node should have @JsonClassDiscriminator", hasDiscriminator)

        // Should have @OptIn(ExperimentalSerializationApi::class) annotation
        val hasOptIn = nodeInterface.annotations.any { ann ->
            ann is AnnotationSpec &&
                ann.typeName.toString() == "kotlin.OptIn"
        }
        assertTrue("Node should have @OptIn", hasOptIn)

        // Should have a User concrete subtype
        val userSubtype = nodeInterface.typeSpecs.find { it.name == "User" }
        assertNotNull("Should have User subtype inside Node", userSubtype)
        assertTrue(
            "User should be a class",
            userSubtype!!.kind == TypeSpec.Kind.CLASS,
        )

        // User subtype should have @SerialName("User")
        val serialNameAnns = userSubtype.annotations
            .filter { it.typeName.toString() == "kotlinx.serialization.SerialName" }
        assertTrue(
            "User subtype should have @SerialName(\"User\")",
            serialNameAnns.isNotEmpty(),
        )

        // User subtype should have __typename field (auto-injected)
        val typenameProp = userSubtype.propertySpecs.find { it.name == "__typename" }
        assertNotNull("User subtype should have __typename property", typenameProp)
        assertTrue(
            "__typename should be non-null String",
            !typenameProp!!.type.isNullable,
        )

        // KDoc should NOT mention global classDiscriminator config
        val kdocText = nodeInterface.kdoc?.toString() ?: ""
        assertTrue(
            "KDoc should NOT mention global classDiscriminator config",
            !kdocText.contains("classDiscriminator"),
        )
    }

    // --- Union type with type-scoped fields ---

    @Test
    fun `union type generates sealed interface with type-scoped fields`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val operation = buildOperation(
            name = "ActivityQuery",
            type = OperationType.QUERY,
            document = fixtureText("fixtures/advanced/unions/ActivityQuery.graphql"),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(anilistIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "ActivityQueryData" }

        // Should have a sealed interface for ActivityUnion
        val activityUnion = dataClass.typeSpecs.find { it.name == "ActivityUnion" }
        assertNotNull("Should have sealed interface for ActivityUnion", activityUnion)
        assertTrue(
            "ActivityUnion should be sealed",
            activityUnion!!.modifiers.contains(KModifier.SEALED),
        )

        // ListActivity should only have status, progress (and __typename)
        val listActivity = activityUnion.typeSpecs.find { it.name == "ListActivity" }
        assertNotNull("Should have ListActivity subtype", listActivity)
        val listFields = listActivity!!.propertySpecs.map { it.name }.toSet()
        assertTrue("ListActivity should have status", "status" in listFields)
        assertTrue("ListActivity should have progress", "progress" in listFields)
        assertTrue("ListActivity should have __typename", "__typename" in listFields)
        assertFalse("ListActivity should NOT have text", "text" in listFields)

        // TextActivity should only have text (and __typename)
        val textActivity = activityUnion.typeSpecs.find { it.name == "TextActivity" }
        assertNotNull("Should have TextActivity subtype", textActivity)
        val textFields = textActivity!!.propertySpecs.map { it.name }.toSet()
        assertTrue("TextActivity should have text", "text" in textFields)
        assertTrue("TextActivity should have __typename", "__typename" in textFields)
        assertFalse("TextActivity should NOT have status", "status" in textFields)
        assertFalse("TextActivity should NOT have progress", "progress" in textFields)
    }

    // --- Explicit __typename ---

    @Test
    fun `explicit __typename selection works without crashing`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document =
            """
            query TypenameQuery {
              viewer {
                __typename
                id
                login
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "TypenameQuery",
            type = OperationType.QUERY,
            document = document,
        )

        // Should not throw
        val selectionSet = parser.parse(operation)

        // viewer's selection set should include __typename
        val viewer = selectionSet.fields.first { it.responseName == "viewer" }
        val typenameField = viewer.selectionSet!!.fields.find { it.schemaName == "__typename" }
        assertNotNull("Should have explicit __typename field", typenameField)
    }

    // --- Alias with path-based identity ---

    @Test
    fun `aliases produce separate per-path classes`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val operation = buildOperation(
            name = "AliasedFields",
            type = OperationType.QUERY,
            document = fixtureText("fixtures/advanced/aliases/AliasedFields.graphql"),
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(anilistIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        assertEquals(1, fileSpecs.size)
        val fileSpec = fileSpecs.first()

        val dataClass = fileSpec.members
            .filterIsInstance<TypeSpec>()
            .firstOrNull { it.name == "AliasedFieldsData" }
        assertNotNull("Should contain AliasedFieldsData class", dataClass)

        // MediaEnglishTitle and MediaNativeTitle should be separate classes
        val mediaEnglishTitle = dataClass!!.typeSpecs.find { it.name == "MediaEnglishTitle" }
        assertNotNull("Should have MediaEnglishTitle class", mediaEnglishTitle)
        val englishFields = mediaEnglishTitle!!.propertySpecs.map { it.name }
        assertTrue("MediaEnglishTitle should have english", "english" in englishFields)

        val mediaNativeTitle = dataClass.typeSpecs.find { it.name == "MediaNativeTitle" }
        assertNotNull("Should have MediaNativeTitle class", mediaNativeTitle)
        val nativeFields = mediaNativeTitle!!.propertySpecs.map { it.name }
        assertTrue("MediaNativeTitle should have native", "native" in nativeFields)

        // No merged MediaTitle class
        val mergedTitleClass = dataClass.typeSpecs.find { it.name == "MediaTitle" }
        assertNull("Should NOT have merged MediaTitle class", mergedTitleClass)
    }

    // --- Helpers ---

    private fun fixtureFile(path: String): File {
        val url = checkNotNull(
            this::class.java.classLoader.getResource(path),
        ) { "Fixture not found: $path" }
        return File(url.toURI())
    }

    private fun fixtureText(path: String): String {
        return fixtureFile(path).readText()
    }

    private fun buildOperation(
        name: String,
        type: OperationType,
        document: String,
    ): GraphQLOperationInfo {
        return GraphQLOperationInfo(
            name = name,
            type = type,
            document = document,
            sourceFile = "$name.graphql",
            variables = emptyList(),
        )
    }
}
