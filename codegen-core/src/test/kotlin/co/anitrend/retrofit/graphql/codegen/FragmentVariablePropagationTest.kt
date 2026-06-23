package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.model.GraphQLFragmentInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.resolve.FragmentVariablePropagator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FragmentVariablePropagationTest {

    private val propagator = FragmentVariablePropagator()

    // ---------------------------------------------------------------------------
    // Auto-propagation: fragment uses a variable the operation doesn't declare
    // ---------------------------------------------------------------------------

    @Test
    fun `should auto-propagate missing fragment variable to operation`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...Status } }",
                sourceFile = "user.graphql",
                variables = emptyList(),
            ),
        )

        val fragments = mapOf(
            "Status" to GraphQLFragmentInfo(
                name = "Status",
                document = "fragment Status on User { name age(format: \$format) }",
                variableUsages = listOf("format"),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        val operation = result.operations.single()
        assertTrue(
            "Operation should have auto-propagated 'format' variable",
            operation.variables.any { it.name == "format" },
        )
        // Document must also be updated with the variable declaration
        assertTrue(
            "Document should contain variable declaration for 'format'",
            operation.document.contains("\$format"),
        )
        assertTrue(
            "Document should declare 'format' as String type",
            operation.document.contains("String"),
        )
    }

    // ---------------------------------------------------------------------------
    // No duplication: operation already declares the variable
    // ---------------------------------------------------------------------------

    @Test
    fun `should not duplicate variable when operation already declares it`() {
        val existingVariable = GraphQLVariableInfo(
            name = "format",
            type = GraphQLType.Named("String", nullable = false),
        )

        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser(\$format: String!) { user { ...Status } }",
                sourceFile = "user.graphql",
                variables = listOf(existingVariable),
            ),
        )

        val fragments = mapOf(
            "Status" to GraphQLFragmentInfo(
                name = "Status",
                document = "fragment Status on User { name age(format: \$format) }",
                variableUsages = listOf("format"),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        val operation = result.operations.single()
        assertEquals(
            "Should have exactly one 'format' variable (no duplicate)",
            1,
            operation.variables.count { it.name == "format" },
        )
        // The existing declaration should be preserved (non-null type)
        val formatVar = operation.variables.first { it.name == "format" }
        assertEquals(
            "Existing variable type should be preserved",
            GraphQLType.Named("String", nullable = false),
            formatVar.type,
        )
    }

    // ---------------------------------------------------------------------------
    // Warning emission
    // ---------------------------------------------------------------------------

    @Test
    fun `should emit warning when auto-propagating missing variable`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...Status } }",
                sourceFile = "user.graphql",
                variables = emptyList(),
            ),
        )

        val fragments = mapOf(
            "Status" to GraphQLFragmentInfo(
                name = "Status",
                document = "fragment Status on User { name age(format: \$format) }",
                variableUsages = listOf("format"),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        assertTrue(
            "Should emit at least one warning",
            result.warnings.isNotEmpty(),
        )
        assertTrue(
            "Warning should mention the variable name 'format'",
            result.warnings.any { it.contains("format") },
        )
        assertTrue(
            "Warning should mention the operation name 'GetUser'",
            result.warnings.any { it.contains("GetUser") },
        )
        assertTrue(
            "Warning should mention the fragment name 'Status'",
            result.warnings.any { it.contains("Status") },
        )
    }

    @Test
    fun `should not emit warning when operation already declares the variable`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser(\$format: String!) { user { ...Status } }",
                sourceFile = "user.graphql",
                variables = listOf(
                    GraphQLVariableInfo(
                        name = "format",
                        type = GraphQLType.Named("String", nullable = false),
                    ),
                ),
            ),
        )

        val fragments = mapOf(
            "Status" to GraphQLFragmentInfo(
                name = "Status",
                document = "fragment Status on User { name age(format: \$format) }",
                variableUsages = listOf("format"),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        assertTrue(
            "No warnings should be emitted when variable is already declared",
            result.warnings.isEmpty(),
        )
    }

    // ---------------------------------------------------------------------------
    // Multiple fragments with different variables
    // ---------------------------------------------------------------------------

    @Test
    fun `should propagate variables from multiple fragments`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...Status ...Profile } }",
                sourceFile = "user.graphql",
                variables = emptyList(),
            ),
        )

        val fragments = mapOf(
            "Status" to GraphQLFragmentInfo(
                name = "Status",
                document = "fragment Status on User { name age(format: \$format) }",
                variableUsages = listOf("format"),
            ),
            "Profile" to GraphQLFragmentInfo(
                name = "Profile",
                document = "fragment Profile on User { avatar(size: \$size) }",
                variableUsages = listOf("size"),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        val operation = result.operations.single()
        assertTrue(
            "Should have 'format' variable from Status fragment",
            operation.variables.any { it.name == "format" },
        )
        assertTrue(
            "Should have 'size' variable from Profile fragment",
            operation.variables.any { it.name == "size" },
        )
        assertEquals(2, operation.variables.size)
    }

    // ---------------------------------------------------------------------------
    // Nested fragments: fragment A spreads B, B uses $var → $var propagated
    // ---------------------------------------------------------------------------

    @Test
    fun `should propagate variables from nested fragments`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...UserFields } }",
                sourceFile = "user.graphql",
                variables = emptyList(),
            ),
        )

        val fragments = mapOf(
            "UserFields" to GraphQLFragmentInfo(
                name = "UserFields",
                document = "fragment UserFields on User { id ...AddressFields }",
                variableUsages = emptyList(),
            ),
            "AddressFields" to GraphQLFragmentInfo(
                name = "AddressFields",
                document = "fragment AddressFields on User { address(zip: \$zip) }",
                variableUsages = listOf("zip"),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        val operation = result.operations.single()
        assertTrue(
            "Should have 'zip' variable from nested AddressFields fragment",
            operation.variables.any { it.name == "zip" },
        )
    }

    // ---------------------------------------------------------------------------
    // No fragments: operations unchanged
    // ---------------------------------------------------------------------------

    @Test
    fun `should return operations unchanged when no fragments are provided`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { id name } }",
                sourceFile = "user.graphql",
                variables = listOf(
                    GraphQLVariableInfo(
                        name = "id",
                        type = GraphQLType.Named("ID", nullable = false),
                    ),
                ),
            ),
        )

        val result = propagator.propagate(operations, emptyMap())

        assertEquals(operations, result.operations)
        assertTrue("No warnings when no fragments", result.warnings.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // Fragment with no variable usages: no change
    // ---------------------------------------------------------------------------

    @Test
    fun `should not propagate when fragment has no variable usages`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...Status } }",
                sourceFile = "user.graphql",
                variables = emptyList(),
            ),
        )

        val fragments = mapOf(
            "Status" to GraphQLFragmentInfo(
                name = "Status",
                document = "fragment Status on User { name age }",
                variableUsages = emptyList(),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        val operation = result.operations.single()
        assertTrue(
            "No variables should be added when fragment has no variable usages",
            operation.variables.isEmpty(),
        )
        assertTrue("No warnings", result.warnings.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // Auto-propagated variable defaults to nullable String
    // ---------------------------------------------------------------------------

    @Test
    fun `should default auto-propagated variable to nullable String`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...Status } }",
                sourceFile = "user.graphql",
                variables = emptyList(),
            ),
        )

        val fragments = mapOf(
            "Status" to GraphQLFragmentInfo(
                name = "Status",
                document = "fragment Status on User { name age(format: \$format) }",
                variableUsages = listOf("format"),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        val operation = result.operations.single()
        val formatVar = operation.variables.first { it.name == "format" }
        assertEquals(
            "Auto-propagated variable should default to String type",
            GraphQLType.Named("String", nullable = true),
            formatVar.type,
        )
    }

    // ---------------------------------------------------------------------------
    // Operation with existing variables: new ones appended
    // ---------------------------------------------------------------------------

    @Test
    fun `should append auto-propagated variables to existing operation variables`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser(\$id: ID!) { user { ...Status } }",
                sourceFile = "user.graphql",
                variables = listOf(
                    GraphQLVariableInfo(
                        name = "id",
                        type = GraphQLType.Named("ID", nullable = false),
                    ),
                ),
            ),
        )

        val fragments = mapOf(
            "Status" to GraphQLFragmentInfo(
                name = "Status",
                document = "fragment Status on User { name age(format: \$format) }",
                variableUsages = listOf("format"),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        val operation = result.operations.single()
        assertEquals(
            "Should have 2 variables: existing 'id' + propagated 'format'",
            2,
            operation.variables.size,
        )
        assertTrue(
            "Existing 'id' variable should be preserved",
            operation.variables.any { it.name == "id" },
        )
        assertTrue(
            "Propagated 'format' variable should be added",
            operation.variables.any { it.name == "format" },
        )
    }

    // ---------------------------------------------------------------------------
    // Multiple operations: each gets its own propagated variables
    // ---------------------------------------------------------------------------

    @Test
    fun `should propagate variables independently for multiple operations`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...Status } }",
                sourceFile = "user.graphql",
                variables = emptyList(),
            ),
            GraphQLOperationInfo(
                name = "GetProfile",
                type = OperationType.QUERY,
                document = "query GetProfile { profile { ...Profile } }",
                sourceFile = "profile.graphql",
                variables = emptyList(),
            ),
        )

        val fragments = mapOf(
            "Status" to GraphQLFragmentInfo(
                name = "Status",
                document = "fragment Status on User { name age(format: \$format) }",
                variableUsages = listOf("format"),
            ),
            "Profile" to GraphQLFragmentInfo(
                name = "Profile",
                document = "fragment Profile on User { avatar(size: \$size) }",
                variableUsages = listOf("size"),
            ),
        )

        val result = propagator.propagate(operations, fragments)

        val getUser = result.operations.first { it.name == "GetUser" }
        val getProfile = result.operations.first { it.name == "GetProfile" }

        assertTrue(
            "GetUser should have 'format' from Status",
            getUser.variables.any { it.name == "format" },
        )
        assertFalse(
            "GetUser should not have 'size'",
            getUser.variables.any { it.name == "size" },
        )
        assertTrue(
            "GetProfile should have 'size' from Profile",
            getProfile.variables.any { it.name == "size" },
        )
        assertFalse(
            "GetProfile should not have 'format'",
            getProfile.variables.any { it.name == "format" },
        )
    }
}