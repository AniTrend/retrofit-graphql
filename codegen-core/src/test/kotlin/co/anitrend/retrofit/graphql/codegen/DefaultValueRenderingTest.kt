package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.generate.GraphQLDefaultValueRenderer
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultValueRenderingTest {
    @Test
    fun `should render enum default literals as enum constants`() {
        val schemaIndex = SchemaIndex.from(listOf(SchemaType.Enum(name = "Status", values = listOf("OPEN", "CLOSED"))))

        val rendered =
            GraphQLDefaultValueRenderer.render(
                defaultValue = "OPEN",
                type = GraphQLType.Named(name = "Status", nullable = false),
                scalarMappings = emptyMap(),
                schemaIndex = schemaIndex,
            )

        assertEquals("Status.OPEN", rendered.toString())
    }

    @Test
    fun `should render enum lists as kotlin list literals`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.Enum(name = "RepositoryAffiliation", values = listOf("OWNER", "COLLABORATOR")),
                ),
            )

        val rendered =
            GraphQLDefaultValueRenderer.render(
                defaultValue = "[OWNER, COLLABORATOR]",
                type = GraphQLType.List(of = GraphQLType.Named(name = "RepositoryAffiliation", nullable = false), nullable = false),
                scalarMappings = emptyMap(),
                schemaIndex = schemaIndex,
            )

        assertEquals(
            "listOf(RepositoryAffiliation.OWNER, RepositoryAffiliation.COLLABORATOR)",
            rendered.toString(),
        )
    }

    @Test
    fun `should render input object defaults with nested enum values`() {
        val schemaIndex =
            SchemaIndex.from(
                listOf(
                    SchemaType.Enum(name = "LabelOrderField", values = listOf("CREATED_AT")),
                    SchemaType.Enum(name = "OrderDirection", values = listOf("ASC", "DESC")),
                    SchemaType.InputObject(
                        name = "LabelOrder",
                        fields =
                            listOf(
                                SchemaType.InputField(
                                    name = "field",
                                    type = GraphQLType.Named(name = "LabelOrderField", nullable = false),
                                ),
                                SchemaType.InputField(
                                    name = "direction",
                                    type = GraphQLType.Named(name = "OrderDirection", nullable = false),
                                ),
                            ),
                    ),
                ),
            )

        val rendered =
            GraphQLDefaultValueRenderer.render(
                defaultValue = "{field : CREATED_AT, direction : ASC}",
                type = GraphQLType.Named(name = "LabelOrder", nullable = false),
                scalarMappings = emptyMap(),
                schemaIndex = schemaIndex,
            )

        assertEquals(
            "LabelOrder(field = LabelOrderField.CREATED_AT, direction = OrderDirection.ASC)",
            rendered.toString(),
        )
    }
}
