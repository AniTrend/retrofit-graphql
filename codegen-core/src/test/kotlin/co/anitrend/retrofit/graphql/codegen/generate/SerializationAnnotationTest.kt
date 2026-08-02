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

import co.anitrend.retrofit.graphql.codegen.config.SerializationBackend
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import co.anitrend.retrofit.graphql.codegen.parser.ResponseSelectionParser
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests that generators emit correct serialization annotations based on
 * the selected [SerializationBackend].
 *
 * Layer B: KotlinPoet source tests. Verifies exact annotation arguments
 * and identifiers in generated source strings.
 */
class SerializationAnnotationTest {

    private val packageName = "com.example"

    // ---- VariableClassGenerator ---- 

    @Test
    fun `variable class with KOTLINX backend emits Serializable and SerialName`() {
        val schemaIndex = SchemaIndex.from(listOf(SchemaType.Scalar(name = "DateTime")))
        val operation = buildOperation(
            "GetItems",
            listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )
        val source =
            VariableClassGenerator.generate(
                operation,
                packageName,
                mapOf("DateTime" to "kotlin.String"),
                schemaIndex,
                SerializationBackend.KOTLINX,
            )!!.toString()

        assertTrue(source.contains("@Serializable"))
        assertTrue(source.contains("@SerialName(\"first\")"))
        assertTrue(source.contains("public val first: Int"))
    }

    @Test
    fun `variable class with GSON backend emits SerializedName`() {
        val schemaIndex = SchemaIndex.EMPTY
        val operation = buildOperation(
            "GetItems",
            listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )
        val source =
            VariableClassGenerator.generate(
                operation,
                packageName,
                emptyMap(),
                schemaIndex,
                SerializationBackend.GSON,
            )!!.toString()

        assertTrue(source.contains("@SerializedName(\"first\")"))
        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
    }

    @Test
    fun `variable class with NONE backend emits no serialization annotations`() {
        val schemaIndex = SchemaIndex.EMPTY
        val operation = buildOperation(
            "GetItems",
            listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )
        val source =
            VariableClassGenerator.generate(
                operation,
                packageName,
                emptyMap(),
                schemaIndex,
                SerializationBackend.NONE,
            )!!.toString()

        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
        assertFalse(source.contains("@SerializedName"))
        assertTrue(source.contains("public val first: Int"))
    }

    @Test
    fun `variable class with keyword field uses escaped kotlinName and annotation with wireName`() {
        val schemaIndex = SchemaIndex.EMPTY
        val operation = buildOperation(
            "GetItems",
            listOf(
                GraphQLVariableInfo("private", GraphQLType.Named(name = "String", nullable = true)),
            ),
        )
        val source =
            VariableClassGenerator.generate(
                operation,
                packageName,
                emptyMap(),
                schemaIndex,
                SerializationBackend.KOTLINX,
            )!!.toString()

        assertTrue(source.contains("@SerialName(\"private\")"))
        assertTrue(source.contains("public val privateValue: String?"))
        assertFalse(source.contains("public val private: String?"))
    }

    // ---- InputObjectGenerator ----

    @Test
    fun `input object with KOTLINX backend emits Serializable and SerialName on all fields`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.InputObject(
                        name = "FilterInput",
                        fields =
                            listOf(
                                SchemaType.InputField(
                                    name = "status",
                                    type = GraphQLType.Named(name = "String", nullable = true),
                                ),
                                SchemaType.InputField(
                                    name = "sortOrder",
                                    type = GraphQLType.Named(name = "String", nullable = true),
                                ),
                            ),
                    ),
                ),
            )
        val source =
            InputObjectGenerator.generate(
                schemaIndex.inputObjects,
                packageName,
                emptyMap(),
                schemaIndex,
                SerializationBackend.KOTLINX,
            ).single().toString()

        assertTrue(source.contains("@Serializable"))
        assertTrue(source.contains("@SerialName(\"status\")"))
        assertTrue(source.contains("@SerialName(\"sortOrder\")"))
    }

    @Test
    fun `input object with GSON backend emits SerializedName`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.InputObject(
                        name = "FilterInput",
                        fields =
                            listOf(
                                SchemaType.InputField(
                                    name = "status",
                                    type = GraphQLType.Named(name = "String", nullable = true),
                                ),
                            ),
                    ),
                ),
            )
        val source =
            InputObjectGenerator.generate(
                schemaIndex.inputObjects,
                packageName,
                emptyMap(),
                schemaIndex,
                SerializationBackend.GSON,
            ).single().toString()

        assertTrue(source.contains("@SerializedName(\"status\")"))
        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
    }

    @Test
    fun `input object with NONE backend emits no serialization annotations`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.InputObject(
                        name = "FilterInput",
                        fields =
                            listOf(
                                SchemaType.InputField(
                                    name = "status",
                                    type = GraphQLType.Named(name = "String", nullable = true),
                                ),
                            ),
                    ),
                ),
            )
        val source =
            InputObjectGenerator.generate(
                schemaIndex.inputObjects,
                packageName,
                emptyMap(),
                schemaIndex,
                SerializationBackend.NONE,
            ).single().toString()

        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
        assertFalse(source.contains("@SerializedName"))
    }

    // ---- EnumGenerator ----

    @Test
    fun `enum with KOTLINX backend emits Serializable and SerialName on constants`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.Enum(name = "Status", values = listOf("OPEN", "CLOSED")),
                ),
            )
        val source =
            EnumGenerator.generate(
                schemaIndex.enums,
                packageName,
                SerializationBackend.KOTLINX,
            ).single().toString()

        assertTrue(source.contains("@Serializable"))
        assertTrue(source.contains("@SerialName(\"OPEN\")"))
        assertTrue(source.contains("@SerialName(\"CLOSED\")"))
        assertTrue(source.contains("OPEN"))
        assertTrue(source.contains("CLOSED"))
    }

    @Test
    fun `enum with GSON backend emits SerializedName on constants`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.Enum(name = "Status", values = listOf("OPEN", "CLOSED")),
                ),
            )
        val source =
            EnumGenerator.generate(
                schemaIndex.enums,
                packageName,
                SerializationBackend.GSON,
            ).single().toString()

        assertTrue(source.contains("@SerializedName(\"OPEN\")"))
        assertTrue(source.contains("@SerializedName(\"CLOSED\")"))
        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
    }

    @Test
    fun `enum with NONE backend emits no serialization annotations`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.Enum(name = "Status", values = listOf("OPEN", "CLOSED")),
                ),
            )
        val source =
            EnumGenerator.generate(
                schemaIndex.enums,
                packageName,
                SerializationBackend.NONE,
            ).single().toString()

        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
        assertFalse(source.contains("@SerializedName"))
        assertTrue(source.contains("OPEN"))
        assertTrue(source.contains("CLOSED"))
    }

    @Test
    fun `enum with lowercase values uppercases constant but preserves wire name in annotation`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.Enum(name = "Animal", values = listOf("dog", "cat", "bird")),
                ),
            )
        val source =
            EnumGenerator.generate(
                schemaIndex.enums,
                packageName,
                SerializationBackend.KOTLINX,
            ).single().toString()

        assertTrue(source.contains("@SerialName(\"dog\")"))
        assertTrue(source.contains("@SerialName(\"cat\")"))
        assertTrue(source.contains("@SerialName(\"bird\")"))
        assertTrue(source.contains("DOG"))
        assertTrue(source.contains("CAT"))
        assertTrue(source.contains("BIRD"))
        assertFalse(source.contains("dog,"))
        assertFalse(source.contains("cat,"))
        assertFalse(source.contains("bird,"))
    }

    // ---- ResponseModelGenerator (Finding 2) ----

    @Test
    fun `response model with KOTLINX backend emits Serializable and property SerialName annotations`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/simple/schemas/github-simple.graphqls",
            fixtureText("fixtures/simple/queries/GetUser.graphql"),
            "GetUser",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.KOTLINX)
        val operation = buildQueryOperation("GetUser", fixtureText("fixtures/simple/queries/GetUser.graphql"))

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        // Class-level serialization marker
        assertTrue(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName(\"GetUserData\")"))

        // Property annotations on root fields - always emitted when KOTLINX
        assertTrue(source.contains("@SerialName(\"viewer\")"))

        // Nested Viewer class should exist and have @SerialName on its properties
        assertTrue(source.contains("@SerialName(\"id\")"))
        assertTrue(source.contains("@SerialName(\"login\")"))
    }

    @Test
    fun `response model with GSON backend emits SerializedName on properties`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/simple/schemas/github-simple.graphqls",
            fixtureText("fixtures/simple/queries/GetUser.graphql"),
            "GetUser",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.GSON)
        val operation = buildQueryOperation("GetUser", fixtureText("fixtures/simple/queries/GetUser.graphql"))

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        // GSON: SerializedName on properties, NO Serializable/SerialName
        assertTrue(source.contains("@SerializedName(\"viewer\")"))
        assertTrue(source.contains("@SerializedName(\"id\")"))
        assertTrue(source.contains("@SerializedName(\"login\")"))
        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
    }

    @Test
    fun `response model with NONE backend emits no serialization annotations`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/simple/schemas/github-simple.graphqls",
            fixtureText("fixtures/simple/queries/GetUser.graphql"),
            "GetUser",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.NONE)
        val operation = buildQueryOperation("GetUser", fixtureText("fixtures/simple/queries/GetUser.graphql"))

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
        assertFalse(source.contains("@SerializedName"))
        assertTrue(source.contains("GetUserData"))
    }

    @Test
    fun `response model with KOTLINX backend omits class-level SerialName on root data class`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/simple/schemas/github-simple.graphqls",
            fixtureText("fixtures/simple/queries/GetUser.graphql"),
            "GetUser",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.KOTLINX)
        val operation = buildQueryOperation("GetUser", fixtureText("fixtures/simple/queries/GetUser.graphql"))

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        assertFalse(source.contains("@SerialName(\"GetUserData\")"))
    }

    @Test
    fun `response model nested class omits path-qualified class-level SerialName descriptor`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/simple/schemas/github-simple.graphqls",
            fixtureText("fixtures/simple/queries/GetUser.graphql"),
            "GetUser",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.KOTLINX)
        val operation = buildQueryOperation("GetUser", fixtureText("fixtures/simple/queries/GetUser.graphql"))

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        assertFalse(source.contains("@SerialName(\"GetUserData.viewer\")"))
    }

    @Test
    fun `sealed interface has Serializable and JsonClassDiscriminator when KOTLINX`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/advanced/unions/ThreeImplementors.graphqls",
            fixtureText("fixtures/advanced/unions/ThreeImplementors.graphql"),
            "ThreeImplementors",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.KOTLINX)
        val operation = buildQueryOperation(
            "ThreeImplementors",
            fixtureText("fixtures/advanced/unions/ThreeImplementors.graphql"),
        )

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .firstOrNull { it.name == "ThreeImplementorsData" }
        assertNotNull("Should have ThreeImplementorsData", dataClass)

        val sealedIface = dataClass!!.typeSpecs.find {
            it.modifiers.contains(KModifier.SEALED)
        }
        assertNotNull("Should have a sealed interface among: ${dataClass.typeSpecs.map { it.name }}", sealedIface)

        // Check on the full source (more reliable than TypeSpec.toString())
        assertTrue("Full source should contain @Serializable on sealed interface", source.contains("@Serializable"))
        assertTrue(
            "Full source should contain @JsonClassDiscriminator",
            source.contains("JsonClassDiscriminator"),
        )
        assertFalse(source.contains("@SerialName(\"ThreeImplementorsData.search\")"))
    }

    @Test
    fun `concrete subtype has SerialName only when KOTLINX backend`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/advanced/unions/ThreeImplementors.graphqls",
            fixtureText("fixtures/advanced/unions/ThreeImplementors.graphql"),
            "ThreeImplementors",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.KOTLINX)
        val operation = buildQueryOperation(
            "ThreeImplementors",
            fixtureText("fixtures/advanced/unions/ThreeImplementors.graphql"),
        )

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        assertTrue("Success subtype should have @SerialName", source.contains("@SerialName(\"Success\")"))
        assertTrue("Failure subtype should have @SerialName", source.contains("@SerialName(\"Failure\")"))
        assertTrue("Pending subtype should have @SerialName", source.contains("@SerialName(\"Pending\")"))
    }

    @Test
    fun `concrete subtype has NO SerialName when NONE backend`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/advanced/unions/ThreeImplementors.graphqls",
            fixtureText("fixtures/advanced/unions/ThreeImplementors.graphql"),
            "ThreeImplementors",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.NONE)
        val operation = buildQueryOperation(
            "ThreeImplementors",
            fixtureText("fixtures/advanced/unions/ThreeImplementors.graphql"),
        )

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        assertFalse(source.contains("@SerialName"))
        assertFalse(source.contains("@Serializable"))
    }

    @Test
    fun `abstract response with NONE backend emits plain sealed structure without serialization markers`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/advanced/unions/ThreeImplementors.graphqls",
            fixtureText("fixtures/advanced/unions/ThreeImplementors.graphql"),
            "ThreeImplementors",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.NONE)
        val operation = buildQueryOperation(
            "ThreeImplementors",
            fixtureText("fixtures/advanced/unions/ThreeImplementors.graphql"),
        )

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val dataClass = fileSpecs.first().members
            .filterIsInstance<TypeSpec>()
            .firstOrNull { it.name == "ThreeImplementorsData" }
        assertNotNull("Should have ThreeImplementorsData", dataClass)

        val sealedIface = dataClass!!.typeSpecs.find {
            it.modifiers.contains(KModifier.SEALED)
        }
        assertNotNull("Should have a sealed interface among: ${dataClass.typeSpecs.map { it.name }}", sealedIface)
        assertTrue(
            "Sealed interface must stay a plain interface under NONE (no annotations)",
            sealedIface!!.annotations.isEmpty(),
        )
        assertTrue(sealedIface.modifiers.contains(KModifier.PUBLIC))

        val source = fileSpecs.first().toString()
        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
        assertFalse(source.contains("@SerializedName"))
        assertFalse(source.contains("JsonClassDiscriminator"))
        assertFalse(source.contains("OptIn"))
        assertFalse(source.contains("kotlinx.serialization"))
        assertFalse(source.contains("com.google.gson"))
        // Plain sealed subtypes are still emitted for every possible type
        assertTrue(source.contains("public sealed interface Result"))
        assertTrue(source.contains("public data class Success("))
        assertTrue(source.contains("public data class Failure("))
        assertTrue(source.contains("public data class Pending("))
    }

    @Test
    fun `unselected abstract subtype is emitted as plain empty class when NONE backend`() {
        val schemaFile = fixtureFile("fixtures/schemas/activity.graphqls")
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )
        val document = """
            query ActivityQuery {
              activities {
                ... on ListActivity { status progress }
                ... on TextActivity { text }
              }
            }
        """.trimIndent()
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.NONE)
        val operation = buildQueryOperation("ActivityQuery", document)

        val fileSpecs = generator.generate(operation, parseSelectionSetWithIndex(schemaIndex, document, "ActivityQuery"), "com.example")
        val source = fileSpecs.first().toString()

        assertFalse(source.contains("@Serializable"))
        assertFalse(source.contains("@SerialName"))
        assertFalse(source.contains("@SerializedName"))
        assertTrue(source.contains("public sealed interface Activities"))
        // MessageActivity is not selected: emitted as a plain empty class
        assertTrue(source.contains("public class MessageActivity :"))
    }

    // ---- Alias field wireName (Concern 5) ----

    @Test
    fun `aliased field uses response name in SerialName annotation not schema name`() {
        // Use the aliases fixture where fields have different response vs schema names
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/schemas/anilist.graphqls",
            fixtureText("fixtures/advanced/aliases/AliasedFields.graphql"),
            "AliasedFields",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.KOTLINX)
        val operation = buildQueryOperation(
            "AliasedFields",
            fixtureText("fixtures/advanced/aliases/AliasedFields.graphql"),
        )

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        // "englishTitle" is an alias on the "title" field - @SerialName uses the alias
        assertTrue(source.contains("@SerialName(\"englishTitle\")"))
        assertTrue(source.contains("@SerialName(\"nativeTitle\")"))
    }

    @Test
    fun `non-aliased field uses response name in SerialName annotation`() {
        val (schemaIndex, selectionSet) = parseSelectionSet(
            "fixtures/simple/schemas/github-simple.graphqls",
            fixtureText("fixtures/simple/queries/GetUser.graphql"),
            "GetUser",
        )
        val generator = ResponseModelGenerator(schemaIndex, backend = SerializationBackend.KOTLINX)
        val operation = buildQueryOperation("GetUser", fixtureText("fixtures/simple/queries/GetUser.graphql"))

        val fileSpecs = generator.generate(operation, selectionSet, "com.example")
        val source = fileSpecs.first().toString()

        // For non-aliased fields, @SerialName uses the response name
        assertTrue(source.contains("@SerialName(\"id\")"))
        assertTrue(source.contains("@SerialName(\"login\")"))
    }

    // ---- Import isolation for EnumGenerator (Concern 6) ----

    @Test
    fun `enum with KOTLINX backend imports only kotlinx serialization`() {
        val schemaIndex = SchemaIndex.from(
            listOf(SchemaType.Enum("Status", listOf("OPEN"))),
        )
        val source = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.KOTLINX,
        ).single().toString()

        assertTrue(source.contains("import kotlinx.serialization.Serializable"))
        assertTrue(source.contains("import kotlinx.serialization.SerialName"))
        assertFalse(source.contains("com.google.gson"))
    }

    @Test
    fun `enum with GSON backend imports only Gson annotations`() {
        val schemaIndex = SchemaIndex.from(
            listOf(SchemaType.Enum("Status", listOf("OPEN"))),
        )
        val source = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.GSON,
        ).single().toString()

        assertTrue(source.contains("import com.google.gson.annotations.SerializedName"))
        assertFalse(source.contains("kotlinx.serialization"))
    }

    @Test
    fun `enum with NONE backend imports no serialization annotations`() {
        val schemaIndex = SchemaIndex.from(
            listOf(SchemaType.Enum("Status", listOf("OPEN"))),
        )
        val source = EnumGenerator.generate(
            schemaIndex.enums, "com.example", SerializationBackend.NONE,
        ).single().toString()

        assertFalse(source.contains("kotlinx.serialization"))
        assertFalse(source.contains("com.google.gson"))
    }

    // ---- Import isolation for InputObjectGenerator (Concern 6) ----

    @Test
    fun `input object with KOTLINX backend imports only kotlinx serialization`() {
        val schemaIndex = SchemaIndex.from(
            listOf(
                SchemaType.InputObject(
                    "FilterInput",
                    listOf(SchemaType.InputField("name", GraphQLType.Named("String", true))),
                ),
            ),
        )
        val source = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.KOTLINX,
        ).single().toString()

        assertTrue(source.contains("import kotlinx.serialization.Serializable"))
        assertTrue(source.contains("import kotlinx.serialization.SerialName"))
        assertFalse(source.contains("com.google.gson"))
    }

    @Test
    fun `input object with GSON backend imports only Gson annotations`() {
        val schemaIndex = SchemaIndex.from(
            listOf(
                SchemaType.InputObject(
                    "FilterInput",
                    listOf(SchemaType.InputField("name", GraphQLType.Named("String", true))),
                ),
            ),
        )
        val source = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.GSON,
        ).single().toString()

        assertTrue(source.contains("import com.google.gson.annotations.SerializedName"))
        assertFalse(source.contains("kotlinx.serialization"))
    }

    @Test
    fun `input object with NONE backend imports no serialization annotations`() {
        val schemaIndex = SchemaIndex.from(
            listOf(
                SchemaType.InputObject(
                    "FilterInput",
                    listOf(SchemaType.InputField("name", GraphQLType.Named("String", true))),
                ),
            ),
        )
        val source = InputObjectGenerator.generate(
            schemaIndex.inputObjects, "com.example", emptyMap(), schemaIndex, SerializationBackend.NONE,
        ).single().toString()

        assertFalse(source.contains("kotlinx.serialization"))
        assertFalse(source.contains("com.google.gson"))
    }

    // ---- Helpers ----

    private fun buildOperation(
        name: String,
        variables: List<GraphQLVariableInfo> = emptyList(),
    ): GraphQLOperationInfo {
        return GraphQLOperationInfo(
            name = name,
            type = OperationType.QUERY,
            document = "query $name",
            sourceFile = "$name.graphql",
            variables = variables,
        )
    }

    private fun buildQueryOperation(name: String, document: String): GraphQLOperationInfo {
        return GraphQLOperationInfo(
            name = name,
            type = OperationType.QUERY,
            document = document,
            sourceFile = "$name.graphql",
            variables = emptyList(),
        )
    }

    private fun fixtureFile(path: String): File {
        val url = checkNotNull(
            this::class.java.classLoader.getResource(path),
        ) { "Fixture not found: $path" }
        return File(url.toURI())
    }

    private fun fixtureText(path: String): String = fixtureFile(path).readText()

    private fun parseSelectionSet(
        schemaPath: String,
        queryDocument: String,
        operationName: String,
    ): Pair<SchemaIndex, co.anitrend.retrofit.graphql.codegen.model.ResponseSelectionSet> {
        val schemaFile = fixtureFile(schemaPath)
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )
        return schemaIndex to parseSelectionSetWithIndex(schemaIndex, queryDocument, operationName)
    }

    private fun parseSelectionSetWithIndex(
        schemaIndex: SchemaIndex,
        queryDocument: String,
        operationName: String,
    ): co.anitrend.retrofit.graphql.codegen.model.ResponseSelectionSet {
        val parser = ResponseSelectionParser(schemaIndex)
        val operation = GraphQLOperationInfo(
            name = operationName,
            type = OperationType.QUERY,
            document = queryDocument,
            sourceFile = "$operationName.graphql",
            variables = emptyList(),
        )
        return parser.parse(operation)
    }
}
