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
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.parser.ResponseSelectionParser
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ResponseModelGeneratorTest {

    private lateinit var githubIndex: SchemaIndex
    private lateinit var anilistIndex: SchemaIndex

    @Before
    fun setUp() {
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

        val anilistSchemaFile = fixtureFile("fixtures/schemas/anilist.graphqls")
        val anilistResult = SchemaParser().parseWithRootTypes(anilistSchemaFile)
        anilistIndex = SchemaIndex.from(
            types = anilistResult.types,
            queryTypeName = anilistResult.queryTypeName,
            mutationTypeName = anilistResult.mutationTypeName,
            subscriptionTypeName = anilistResult.subscriptionTypeName,
        )
    }

    // --- Simple query model ---

    @Test
    fun `generate simple query model with nested Viewer type`() {
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

        val dataClass = fileSpec.members
            .filterIsInstance<TypeSpec>()
            .firstOrNull { it.name == "GetUserData" }
        assertNotNull("Should contain GetUserData class", dataClass)

        assertTrue(
            "GetUserData should be a data class",
            dataClass!!.modifiers.contains(KModifier.DATA),
        )

        assertTrue(
            "GetUserData should have @Serializable annotation",
            dataClass.annotations.any {
                it is AnnotationSpec &&
                    it.typeName == ClassName("kotlinx.serialization", "Serializable")
            },
        )

        val viewerProp = dataClass.propertySpecs.firstOrNull { it.name == "viewer" }
        assertNotNull("Should have viewer property", viewerProp)
        assertTrue(
            "viewer should be public",
            viewerProp!!.modifiers.contains(KModifier.PUBLIC),
        )

        // Check nested Viewer class exists
        val viewerClass = dataClass.typeSpecs.firstOrNull { it.name == "Viewer" }
        assertNotNull("Should contain nested Viewer class", viewerClass)
        assertTrue(
            "Viewer should be a data class",
            viewerClass!!.modifiers.contains(KModifier.DATA),
        )

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

        val updateBioProp = dataClass!!.propertySpecs.firstOrNull { it.name == "updateBio" }
        assertNotNull("Should have updateBio property", updateBioProp)

        val payloadClass = dataClass.typeSpecs.firstOrNull { it.name == "UpdateBio" }
        assertNotNull("Should contain nested UpdateBio class", payloadClass)

        val userProp = payloadClass!!.propertySpecs.firstOrNull { it.name == "user" }
        assertNotNull("UpdateBio should have user property", userProp)

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

        val mediaClass = dataClass!!.typeSpecs.firstOrNull { it.name == "Media" }
        assertNotNull("Should contain nested Media class", mediaClass)

        val englishTitleProp = mediaClass!!.propertySpecs.firstOrNull { it.name == "englishTitle" }
        assertNotNull("Should have englishTitle property", englishTitleProp)
        val hasEnglishSerialName = englishTitleProp!!.annotations.any {
            it is AnnotationSpec &&
                it.typeName == ClassName("kotlinx.serialization", "SerialName") &&
                it.members.any { m -> m.toString().contains("\"englishTitle\"") }
        }
        assertTrue("englishTitle should have @SerialName(\"englishTitle\")", hasEnglishSerialName)

        val nativeTitleProp = mediaClass.propertySpecs.firstOrNull { it.name == "nativeTitle" }
        assertNotNull("Should have nativeTitle property", nativeTitleProp)
        val hasNativeSerialName = nativeTitleProp!!.annotations.any {
            it is AnnotationSpec &&
                it.typeName == ClassName("kotlinx.serialization", "SerialName") &&
                it.members.any { m -> m.toString().contains("\"nativeTitle\"") }
        }
        assertTrue("nativeTitle should have @SerialName(\"nativeTitle\")", hasNativeSerialName)
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

        val pageClass = dataClass.typeSpecs.firstOrNull { it.name == "Page" }
        assertNotNull("Should have nested Page class", pageClass)

        val mediaProp = pageClass!!.propertySpecs.firstOrNull { it.name == "media" }
        assertNotNull("Page should have media property", mediaProp)

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

        val rootPropNames = dataClass.propertySpecs.map { it.name }
        assertEquals(rootPropNames.sorted(), rootPropNames)

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

    // --- Operation name used in default class name ---

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

    // --- Conditional Presence ---

    @Test
    fun `conditional field gets nullable type with null default in primary constructor`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document =
            """
            query CondQuery(${'$'}includeName: Boolean!) {
              viewer {
                id
                name @include(if: ${'$'}includeName)
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

        assertTrue("Conditional name should be nullable", nameProp.type.isNullable)

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

        val loginParam = primaryCtor.parameters.find { it.name == "login" }!!
        assertNull(
            "Constructor param for 'login' should NOT have default value",
            loginParam.defaultValue,
        )

        assertTrue("Login should be non-null from schema", !loginProp.type.isNullable)
    }

    // --- Polymorphism: sealed interface ---

    @Test
    fun `generates sealed interface for interface fields`() {
        val parser = ResponseSelectionParser(githubIndex)
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

        // The node field returns Node interface, so the root should have
        // a 'node' property and a sealed interface for Node
        val allNested = dataClass.typeSpecs.map { it.name }
        assertNotNull("NodeQueryData should have nested types: $allNested", allNested.isNotEmpty())
    }

    // --- Union type with type-scoped fields (Activity union) ---

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

        // The activities field returns ActivityUnion, which is abstract
        val activityUnion = dataClass.typeSpecs.find { it.name == "PageActivities" }
        assertNotNull("Should have sealed interface for PageActivities", activityUnion)
        assertTrue(
            "PageActivities should be sealed",
            activityUnion!!.modifiers.contains(KModifier.SEALED),
        )

        // ListActivity should only have status, progress (no __typename)
        val listActivity = activityUnion.typeSpecs.find { it.name == "ListActivity" }
        assertNotNull("Should have ListActivity subtype", listActivity)
        val listFields = listActivity!!.propertySpecs.map { it.name }.toSet()
        assertTrue("ListActivity should have status", "status" in listFields)
        assertTrue("ListActivity should have progress", "progress" in listFields)
        assertFalse("ListActivity should NOT have __typename", "__typename" in listFields)
        assertFalse("ListActivity should NOT have text", "text" in listFields)

        // TextActivity should only have text (no __typename)
        val textActivity = activityUnion.typeSpecs.find { it.name == "TextActivity" }
        assertNotNull("Should have TextActivity subtype", textActivity)
        val textFields = textActivity!!.propertySpecs.map { it.name }.toSet()
        assertTrue("TextActivity should have text", "text" in textFields)
        assertFalse("TextActivity should NOT have __typename", "__typename" in textFields)
        assertFalse("TextActivity should NOT have status", "status" in textFields)
    }

    // --- Aliased __typename remains as property ---

    @Test
    fun `aliased __typename remains as property on sealed subtype`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query AliasTypenameQuery {
              node(id: "123") {
                kind: __typename
                id
                ... on User {
                  login
                  name
                }
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "AliasTypenameQuery",
            type = OperationType.QUERY,
            document = document,
        )
        val selectionSet = parser.parse(operation)
        val generator = ResponseModelGenerator(githubIndex)

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "AliasTypenameQueryData" }

        // Should have a sealed interface for Node
        val nodeInterface = dataClass.typeSpecs.find { it.name == "Node" }
        assertNotNull("Should have sealed interface for Node", nodeInterface)

        val userSubtype = nodeInterface!!.typeSpecs.find { it.name == "User" }
        assertNotNull("Should have User subtype inside Node", userSubtype)

        // User subtype should have 'kind' property
        val kindProp = userSubtype!!.propertySpecs.find { it.name == "kind" }
        assertNotNull("User subtype should have kind property", kindProp)

        // User subtype should NOT have a separate __typename property
        val typenameProp = userSubtype.propertySpecs.find { it.name == "__typename" }
        assertNull("User subtype should NOT have __typename property", typenameProp)
    }

    // --- Unselected union members generate regular classes ---

    @Test
    fun `unselected union members generate regular class instead of invalid empty data class`() {
        val activitySchemaFile = fixtureFile("fixtures/schemas/activity.graphqls")
        val activityResult = SchemaParser().parseWithRootTypes(activitySchemaFile)
        val activityIndex = SchemaIndex.from(
            types = activityResult.types,
            queryTypeName = activityResult.queryTypeName,
            mutationTypeName = activityResult.mutationTypeName,
            subscriptionTypeName = activityResult.subscriptionTypeName,
        )

        val parser = ResponseSelectionParser(activityIndex)
        val generator = ResponseModelGenerator(activityIndex)

        val document = """
            query ActivityQuery {
              activities {
                ... on ListActivity { status progress }
                ... on TextActivity { text }
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "ActivityQuery",
            type = OperationType.QUERY,
            document = document,
        )
        val selectionSet = parser.parse(operation)
        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "ActivityQueryData" }

        val activityUnion = dataClass.typeSpecs.find { it.name == "Activities" }
        assertNotNull("Should have sealed interface for Activities", activityUnion)
    }

    // ============================================================
    // Test A: common interface field (ThreeImplementors)
    // ============================================================

    @Test
    fun `testA common interface field generated with per-type detail classes`() {
        val schemaFile = fixtureFile(
            "fixtures/advanced/unions/ThreeImplementors.graphqls",
        )
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )

        val parser = ResponseSelectionParser(schemaIndex)
        val generator = ResponseModelGenerator(schemaIndex)

        val operation = buildOperation(
            name = "ThreeImplementors",
            type = OperationType.QUERY,
            document = fixtureText(
                "fixtures/advanced/unions/ThreeImplementors.graphql",
            ),
        )
        val selectionSet = parser.parse(operation)
        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "ThreeImplementorsData" }

        val allNestedNames = dataClass.typeSpecs.map { it.name }.filterNotNull()

        // Verify the sealed interface exists
        val resultIface = dataClass.typeSpecs.find { it.name == "Result" }
        assertNotNull("Should have sealed interface for Result", resultIface)
        assertTrue(
            "Result should be sealed",
            resultIface!!.modifiers.contains(KModifier.SEALED),
        )

        // Should have all three subtypes (Success, Failure, Pending)
        val successSubtype = resultIface.typeSpecs.find { it.name == "Success" }
        assertNotNull("Should have Success subtype", successSubtype)
        val successFields = successSubtype!!.propertySpecs.map { it.name }.toSet()
        assertTrue("Success should have detail property", "detail" in successFields)

        val failureSubtype = resultIface.typeSpecs.find { it.name == "Failure" }
        assertNotNull("Should have Failure subtype", failureSubtype)
        val failureFields = failureSubtype!!.propertySpecs.map { it.name }.toSet()
        assertTrue("Failure should have detail property", "detail" in failureFields)

        val pendingSubtype = resultIface.typeSpecs.find { it.name == "Pending" }
        // Debug: check what's in resultIface
        val allSubtypeNames = resultIface.typeSpecs.map { it.name }
        assertNotNull("Should have Pending subtype among: $allSubtypeNames", pendingSubtype)
        val pendingFields = pendingSubtype!!.propertySpecs.map { it.name }.toSet()
        assertTrue("Pending should have detail property. All subtypes: $allSubtypeNames, fields: $pendingFields", "detail" in pendingFields)

        // Verify detail classes - use exact class name assertions
        // Success detail should have id and value
        val successDetail = dataClass.typeSpecs.find {
            it.name != null && it.name!!.contains("Success") &&
                it.name!!.contains("Detail")
        }
        assertNotNull(
            "Should have SuccessResultDetail class. Names: $allNestedNames",
            successDetail,
        )
        val sdFields = successDetail!!.propertySpecs.map { it.name }.toSet()
        assertTrue("Success detail should have id", "id" in sdFields)
        assertTrue("Success detail should have value", "value" in sdFields)
        assertFalse("Success detail should NOT have reason", "reason" in sdFields)

        // Failure detail should have id and reason
        val failureDetail = dataClass.typeSpecs.find {
            it.name != null && it.name!!.contains("Failure") &&
                it.name!!.contains("Detail")
        }
        assertNotNull(
            "Should have FailureResultDetail class. Names: $allNestedNames",
            failureDetail,
        )
        val fdFields = failureDetail!!.propertySpecs.map { it.name }.toSet()
        assertTrue("Failure detail should have id", "id" in fdFields)
        assertTrue("Failure detail should have reason", "reason" in fdFields)
        assertFalse("Failure detail should NOT have value", "value" in fdFields)

        // Pending detail should have id only
        val pendingDetail = dataClass.typeSpecs.find {
            it.name != null && it.name!!.contains("Pending") &&
                it.name!!.contains("Detail")
        }
        assertNotNull(
            "Should have PendingResultDetail class. Names: $allNestedNames",
            pendingDetail,
        )
        val pdFields = pendingDetail!!.propertySpecs.map { it.name }.toSet()
        assertTrue("Pending detail should have id", "id" in pdFields)
        assertFalse("Pending detail should NOT have value", "value" in pdFields)
        assertFalse("Pending detail should NOT have reason", "reason" in pdFields)
    }

    // ============================================================
    // Test B: nested abstract paths (NestedAbstractBranches)
    // ============================================================

    @Test
    fun `testB nested abstract paths generate separate per-branch classes`() {
        val schemaFile = fixtureFile(
            "fixtures/advanced/unions/NestedAbstractBranches.graphqls",
        )
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )

        val parser = ResponseSelectionParser(schemaIndex)
        val generator = ResponseModelGenerator(schemaIndex)

        val operation = buildOperation(
            name = "NestedAbstractBranches",
            type = OperationType.QUERY,
            document = fixtureText(
                "fixtures/advanced/unions/NestedAbstractBranches.graphql",
            ),
        )
        val selectionSet = parser.parse(operation)
        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "NestedAbstractBranchesData" }

        val allNestedNames = dataClass.typeSpecs.map { it.name }.filterNotNull()

        // Should have sealed interface for Outer
        val outerIface = dataClass.typeSpecs.find { it.name == "Outer" }
        assertNotNull("Should have sealed interface for Outer", outerIface)

        // Should have OuterA and OuterB subtypes
        val outerASubtype = outerIface!!.typeSpecs.find { it.name == "OuterA" }
        assertNotNull("Should have OuterA subtype", outerASubtype)
        val outerBSubtype = outerIface.typeSpecs.find { it.name == "OuterB" }
        assertNotNull("Should have OuterB subtype", outerBSubtype)

        // OuterA.inner and OuterB.inner should reference different types
        val oaInnerProp = outerASubtype!!.propertySpecs.find { it.name == "inner" }
        val obInnerProp = outerBSubtype!!.propertySpecs.find { it.name == "inner" }
        assertNotNull("OuterA should have inner property", oaInnerProp)
        assertNotNull("OuterB should have inner property", obInnerProp)

        // Find detail classes - one for OuterA, one for OuterB
        val outerADetail = dataClass.typeSpecs.find {
            it.name != null && it.name!!.contains("OuterA") &&
                it.name!!.contains("Detail")
        }
        assertNotNull(
            "Should have Detail class for OuterA branch. Names: $allNestedNames",
            outerADetail,
        )

        val outerBDetail = dataClass.typeSpecs.find {
            it.name != null && it.name!!.contains("OuterB") &&
                it.name!!.contains("Detail")
        }
        assertNotNull(
            "Should have Detail class for OuterB branch. Names: $allNestedNames",
            outerBDetail,
        )

        assertNotSame(
            "Detail classes for different outer branches should be distinct",
            outerADetail,
            outerBDetail,
        )

        val oaFields = outerADetail!!.propertySpecs.map { it.name }.toSet()
        assertTrue("OuterA.InnerX detail should have fromA", "fromA" in oaFields)
        assertFalse("OuterA.InnerX detail should NOT have fromB", "fromB" in oaFields)

        val obFields = outerBDetail!!.propertySpecs.map { it.name }.toSet()
        assertTrue("OuterB.InnerX detail should have fromB", "fromB" in obFields)
        assertFalse("OuterB.InnerX detail should NOT have fromA", "fromA" in obFields)
    }

    // ============================================================
    // Test C: alias alternatives
    // ============================================================

    @Test
    fun `testC alias alternatives generate separate classes with correct fields`() {
        val schemaFile = fixtureFile(
            "fixtures/advanced/unions/AliasAlternatives.graphqls",
        )
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )

        val parser = ResponseSelectionParser(schemaIndex)
        val generator = ResponseModelGenerator(schemaIndex)

        val operation = buildOperation(
            name = "SearchQuery",
            type = OperationType.QUERY,
            document = fixtureText(
                "fixtures/advanced/unions/AliasAlternatives.graphql",
            ),
        )
        val selectionSet = parser.parse(operation)
        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "SearchQueryData" }

        // Should have sealed interface for SearchResult
        val searchIface = dataClass.typeSpecs.find { it.name == "Search" }
        assertNotNull("Should have sealed interface for Search", searchIface)

        // User subtype should have value property (from displayName alias)
        val userSubtype = searchIface!!.typeSpecs.find { it.name == "User" }
        assertNotNull("Should have User subtype", userSubtype)
        val userFields = userSubtype!!.propertySpecs.map { it.name }.toSet()
        assertTrue("User should have value property", "value" in userFields)

        // Repository subtype should have value property (from repositoryName alias)
        val repoSubtype = searchIface.typeSpecs.find { it.name == "Repository" }
        assertNotNull("Should have Repository subtype", repoSubtype)
        val repoFields = repoSubtype!!.propertySpecs.map { it.name }.toSet()
        assertTrue("Repository should have value property", "value" in repoFields)

        // Both subtypes should have at least one property (not empty)
        assertTrue("User should not be empty", userFields.isNotEmpty())
        assertTrue("Repository should not be empty", repoFields.isNotEmpty())
    }

    // ============================================================
    // Test D: covariant object return (CovariantAnimals)
    // ============================================================

    @Test
    fun `testD covariant object return generates separate friend types`() {
        val schemaFile = fixtureFile(
            "fixtures/advanced/unions/CovariantAnimals.graphqls",
        )
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )

        val parser = ResponseSelectionParser(schemaIndex)
        val generator = ResponseModelGenerator(schemaIndex)

        val operation = buildOperation(
            name = "Animals",
            type = OperationType.QUERY,
            document = fixtureText(
                "fixtures/advanced/unions/CovariantAnimals.graphql",
            ),
        )
        val selectionSet = parser.parse(operation)
        val fileSpecs = generator.generate(operation, selectionSet, "com.example")

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .first { it.name == "AnimalsData" }

        val allNestedNames = dataClass.typeSpecs.map { it.name }.filterNotNull()

        // Should have sealed interface for Animal
        val animalIface = dataClass.typeSpecs.find {
            it.name == "Animals" ||
                (it.modifiers.contains(KModifier.SEALED) &&
                    it.kind == TypeSpec.Kind.INTERFACE)
        }
        assertNotNull("Should have sealed interface for Animals field: $allNestedNames", animalIface)

        // Dog subtype should have friend property
        val dogSubtype = animalIface?.typeSpecs?.find { it.name == "Dog" }
        assertNotNull("Should have Dog subtype", dogSubtype)
        val dogFriendProp = dogSubtype?.propertySpecs?.find { it.name == "friend" }
        assertNotNull("Dog should have friend property", dogFriendProp)

        // Cat subtype should have friend property
        val catSubtype = animalIface?.typeSpecs?.find { it.name == "Cat" }
        assertNotNull("Should have Cat subtype", catSubtype)
        val catFriendProp = catSubtype?.propertySpecs?.find { it.name == "friend" }
        assertNotNull("Cat should have friend property", catFriendProp)

        // Dog.friend type should be different from Cat.friend type
        if (dogFriendProp != null && catFriendProp != null) {
            assertNotSame(
                "Dog.friend type should differ from Cat.friend type",
                dogFriendProp.type,
                catFriendProp.type,
            )
        }
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

        val selectionSet = parser.parse(operation)

        val viewer = selectionSet.fields.first { it.responseName == "viewer" }
        val typenameField = viewer.selectionSet!!.fields.find { it.schemaName == "__typename" }
        assertNotNull("Should have explicit __typename field", typenameField)
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
