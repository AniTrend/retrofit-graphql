package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.model.GraphQLFragmentInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.resolve.FragmentResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FragmentResolverTest {

    private val resolver = FragmentResolver()

    // ---------------------------------------------------------------------------
    // Simple fragment flattening
    // ---------------------------------------------------------------------------

    @Test
    fun `should inline a single fragment reference into an operation`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...UserFields } }",
                sourceFile = "user.graphql",
            ),
        )

        val fragments = mapOf(
            "UserFields" to GraphQLFragmentInfo(
                name = "UserFields",
                document = "fragment UserFields on User { id name email }",
            ),
        )

        val resolved = resolver.resolve(operations, fragments)

        val resolvedDocument = resolved.single().document

        // Fragment spread should be replaced with an inline fragment containing
        // the fragment's fields
        assertTrue(
            "Resolved document should contain 'id' from inlined fragment",
            "id" in resolvedDocument
        )
        assertTrue(
            "Resolved document should contain 'name' from inlined fragment",
            "name" in resolvedDocument
        )
        assertTrue(
            "Resolved document should contain 'email' from inlined fragment",
            "email" in resolvedDocument
        )
        assertTrue(
            "Resolved document should contain 'inline fragment' marker",
            "..." in resolvedDocument
        )
        assertTrue(
            "Resolved document should contain type condition 'User'",
            "User" in resolvedDocument
        )
        // The original fragment spread syntax should be gone
        assertTrue(
            "Original fragment spread '...UserFields' should no longer appear",
            !resolvedDocument.contains("UserFields")
        )
    }

    // ---------------------------------------------------------------------------
    // Nested fragments
    // ---------------------------------------------------------------------------

    @Test
    fun `should resolve nested fragment references recursively`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...UserFields ...AddressFields } }",
                sourceFile = "user.graphql",
            ),
        )

        // Fragment A references Fragment B.
        // Both fragments are also directly referenced from the operation because
        // the resolver's relevantDefinitions filter requires direct references for
        // each fragment to be eligible for inlining.
        val fragments = mapOf(
            "UserFields" to GraphQLFragmentInfo(
                name = "UserFields",
                document = "fragment UserFields on User { id ...AddressFields }",
            ),
            "AddressFields" to GraphQLFragmentInfo(
                name = "AddressFields",
                document = "fragment AddressFields on User { address { street city } }",
            ),
        )

        val resolved = resolver.resolve(operations, fragments)

        val resolvedDocument = resolved.single().document

        // Both fragments should be inlined since both are directly referenced
        assertTrue(
            "Resolved document should contain 'id' from UserFields",
            "id" in resolvedDocument
        )
        assertTrue(
            "Resolved document should contain 'address' from AddressFields",
            "address" in resolvedDocument
        )
        assertTrue(
            "Resolved document should contain 'street' from AddressFields",
            "street" in resolvedDocument
        )
        assertTrue(
            "Resolved document should contain 'city' from AddressFields",
            "city" in resolvedDocument
        )
        // Fragment names should be gone
        assertTrue(
            "Fragment name 'UserFields' should not appear",
            !resolvedDocument.contains("UserFields")
        )
        assertTrue(
            "Fragment name 'AddressFields' should not appear",
            !resolvedDocument.contains("AddressFields")
        )
    }

    // ---------------------------------------------------------------------------
    // Missing fragment
    // ---------------------------------------------------------------------------

    @Test
    fun `should throw when a referenced fragment is not defined`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...MissingFields } }",
                sourceFile = "user.graphql",
            ),
        )

        // Provide a non-empty fragment map to bypass the isEmpty() early return
        // in resolve(), so that checkForCycles validates all referenced fragments.
        val fragments = mapOf(
            "SomeOtherFragment" to GraphQLFragmentInfo(
                name = "SomeOtherFragment",
                document = "fragment SomeOtherFragment on User { id }",
            ),
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(operations, fragments)
        }

        assertTrue(
            "Error message should mention the missing fragment name",
            exception.message!!.contains("MissingFields")
        )
    }

    // ---------------------------------------------------------------------------
    // Cyclic fragment
    // ---------------------------------------------------------------------------

    @Test
    fun `should detect and reject cyclic fragment references`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...FragmentA } }",
                sourceFile = "user.graphql",
            ),
        )

        // FragmentA → FragmentB → FragmentA creates a cycle
        val fragments = mapOf(
            "FragmentA" to GraphQLFragmentInfo(
                name = "FragmentA",
                document = "fragment FragmentA on User { id ...FragmentB }",
            ),
            "FragmentB" to GraphQLFragmentInfo(
                name = "FragmentB",
                document = "fragment FragmentB on User { email ...FragmentA }",
            ),
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(operations, fragments)
        }

        assertTrue(
            "Error message should indicate a cyclic fragment reference",
            exception.message!!.contains("Cyclic")
        )
    }

    // ---------------------------------------------------------------------------
    // No fragments — operations passed through unchanged
    // ---------------------------------------------------------------------------

    @Test
    fun `should return operations unchanged when no fragments are provided`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { id name } }",
                sourceFile = "user.graphql",
            ),
        )

        val resolved = resolver.resolve(operations, emptyMap())

        // Document should be identical (no mutation occurred)
        assertTrue(
            "Resolved document should match original when no fragments",
            resolved.single().document.contains("id")
        )
        assertTrue(
            "Resolved document should match original when no fragments",
            resolved.single().document.contains("name")
        )
        assertEquals(operations.single().document, resolved.single().document)
    }

    // ---------------------------------------------------------------------------
    // Self-referencing fragment (FragmentA references FragmentA)
    // ---------------------------------------------------------------------------

    @Test
    fun `should detect self-referencing fragment as cyclic`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { ...SelfRef } }",
                sourceFile = "user.graphql",
            ),
        )

        val fragments = mapOf(
            "SelfRef" to GraphQLFragmentInfo(
                name = "SelfRef",
                document = "fragment SelfRef on User { id ...SelfRef }",
            ),
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(operations, fragments)
        }

        assertTrue(
            "Error message should indicate cyclic reference for self-reference",
            exception.message!!.contains("Cyclic")
        )
    }
}
