package co.anitrend.retrofit.graphql.model.request

import co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables
import co.anitrend.retrofit.graphql.model.GraphQLValue
import co.anitrend.retrofit.graphql.model.GraphQLVariables
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GraphQLOperationRequestTest {

    private data class GetUserVariables(
        val login: String,
    ) : GraphQLVariables

    @Test
    fun `constructs with query operation name and variables`() {
        val request =
            GraphQLOperationRequest<GetUserVariables>(
                query = "query GetUser(\$login: String!) { user(login: \$login) { login } }",
                operationName = "GetUser",
                variables = GetUserVariables(login = "octocat"),
            )

        assertEquals("query GetUser(\$login: String!) { user(login: \$login) { login } }", request.query)
        assertEquals("GetUser", request.operationName)
        assertEquals(GetUserVariables("octocat"), request.variables)
        assertNull(request.extensions)
    }

    @Test
    fun `variables default to null`() {
        val request =
            GraphQLOperationRequest<EmptyGraphQLVariables>(
                query = "query GetCurrentUser { viewer { login } }",
                operationName = "GetCurrentUser",
            )

        assertEquals("GetCurrentUser", request.operationName)
        assertNull(request.variables)
    }

    @Test
    fun `copy derives a new request without mutating the original`() {
        val request =
            GraphQLOperationRequest<EmptyGraphQLVariables>(
                query = "query A { a }",
                operationName = "A",
            )

        val renamed = request.copy(operationName = "B")

        assertEquals("B", renamed.operationName)
        assertEquals("A", request.operationName)
        assertEquals("query A { a }", request.query)
    }

    @Test
    fun `withPersistedQuery adds the neutral extension shape`() {
        val request =
            GraphQLOperationRequest<EmptyGraphQLVariables>(
                query = "query GetCurrentUser { viewer { login } }",
                operationName = "GetCurrentUser",
            )

        val withPersistedQuery = request.withPersistedQuery(sha256Hash = "abc123")

        val persistedQuery = withPersistedQuery.extensions?.fields?.get("persistedQuery") as GraphQLValue.ObjectValue
        assertEquals(GraphQLValue.StringValue("abc123"), persistedQuery.fields["sha256Hash"])
        assertEquals(GraphQLValue.NumberValue(BigDecimal(1)), persistedQuery.fields["version"])
    }

    @Test
    fun `withPersistedQuery honours a custom version`() {
        val request =
            GraphQLOperationRequest<EmptyGraphQLVariables>(
                query = "query GetCurrentUser { viewer { login } }",
                operationName = "GetCurrentUser",
            )

        val withPersistedQuery = request.withPersistedQuery(sha256Hash = "def456", version = 2)

        val persistedQuery = withPersistedQuery.extensions?.fields?.get("persistedQuery") as GraphQLValue.ObjectValue
        assertEquals(GraphQLValue.NumberValue(BigDecimal(2)), persistedQuery.fields["version"])
    }

    @Test
    fun `withPersistedQuery keeps the original request untouched`() {
        val request =
            GraphQLOperationRequest<EmptyGraphQLVariables>(
                query = "query GetCurrentUser { viewer { login } }",
                operationName = "GetCurrentUser",
                extensions =
                    GraphQLValue.ObjectValue(
                        fields = mapOf("trace" to GraphQLValue.BooleanValue(true)),
                    ),
            )

        val withPersistedQuery = request.withPersistedQuery(sha256Hash = "abc123")

        assertEquals(2, withPersistedQuery.extensions?.fields?.size)
        assertTrue(withPersistedQuery.extensions?.fields?.containsKey("persistedQuery") == true)
        assertEquals(GraphQLValue.BooleanValue(true), withPersistedQuery.extensions?.fields?.get("trace"))
        assertEquals(
            GraphQLValue.ObjectValue(fields = mapOf("trace" to GraphQLValue.BooleanValue(true))),
            request.extensions,
        )
    }
}
