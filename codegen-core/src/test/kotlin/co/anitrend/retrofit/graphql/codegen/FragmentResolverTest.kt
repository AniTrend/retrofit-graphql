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
        // Both fragments are directly referenced from the operation.
        // With transitive resolution, AddressFields would also be resolved
        // even without the direct spread, but this test exercises both paths.
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
        // in resolve(), so that computeReachableFragments validates all referenced fragments.
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

    // ---------------------------------------------------------------------------
    // Transitive nested fragments (the core fix for issue #423)
    // ---------------------------------------------------------------------------

    @Test
    fun `should resolve transitive nested fragments when operation only references parent`() {
        // The key scenario from issue #423: operation references only the parent
        // fragment, which internally references a child fragment. The child must
        // be transitively resolved even though it is not directly referenced by
        // the operation.
        //
        // Before the fix: relevantDefinitions only included fragments directly
        // referenced by the operation document, so nested fragments were left as
        // unresolved spread names.
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetMedia",
                type = OperationType.QUERY,
                document = "query GetMedia { Media(id: 1) { ...MediaCoreFragment } }",
                sourceFile = "media.graphql",
            ),
        )

        val fragments = mapOf(
            "MediaCoreFragment" to GraphQLFragmentInfo(
                name = "MediaCoreFragment",
                document = "fragment MediaCoreFragment on Media { id title { ...MediaTitleFragment } }",
            ),
            "MediaTitleFragment" to GraphQLFragmentInfo(
                name = "MediaTitleFragment",
                document = "fragment MediaTitleFragment on MediaTitle { romaji english }",
            ),
        )

        val resolved = resolver.resolve(operations, fragments)

        val resolvedDocument = resolved.single().document

        // Parent fragment fields should be present
        assertTrue(
            "Resolved document should contain 'id' from MediaCoreFragment",
            "id" in resolvedDocument
        )
        assertTrue(
            "Resolved document should contain 'title' from MediaCoreFragment",
            "title" in resolvedDocument
        )

        // Child fragment fields should be transitively resolved
        assertTrue(
            "Resolved document should contain 'romaji' from MediaTitleFragment",
            "romaji" in resolvedDocument
        )
        assertTrue(
            "Resolved document should contain 'english' from MediaTitleFragment",
            "english" in resolvedDocument
        )

        // Fragment spread names should be fully inlined
        assertTrue(
            "Fragment name 'MediaCoreFragment' should not appear in resolved document",
            !resolvedDocument.contains("MediaCoreFragment")
        )
        assertTrue(
            "Fragment name 'MediaTitleFragment' should not appear in resolved document",
            !resolvedDocument.contains("MediaTitleFragment")
        )
    }

    // ---------------------------------------------------------------------------
    // Deep transitive fragment chain (3+ levels)
    // ---------------------------------------------------------------------------

    @Test
    fun `should resolve deep transitive fragment chain`() {
        // Operation -> FragmentA -> FragmentB -> FragmentC
        // All three levels should be inlined
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetDeep",
                type = OperationType.QUERY,
                document = "query GetDeep { item { ...FragmentA } }",
                sourceFile = "deep.graphql",
            ),
        )

        val fragments = mapOf(
            "FragmentA" to GraphQLFragmentInfo(
                name = "FragmentA",
                document = "fragment FragmentA on Item { a ...FragmentB }",
            ),
            "FragmentB" to GraphQLFragmentInfo(
                name = "FragmentB",
                document = "fragment FragmentB on Item { b ...FragmentC }",
            ),
            "FragmentC" to GraphQLFragmentInfo(
                name = "FragmentC",
                document = "fragment FragmentC on Item { c }",
            ),
        )

        val resolved = resolver.resolve(operations, fragments)

        val resolvedDocument = resolved.single().document

        assertTrue("Deep chain: 'a' should be resolved", "a" in resolvedDocument)
        assertTrue("Deep chain: 'b' should be resolved", "b" in resolvedDocument)
        assertTrue("Deep chain: 'c' should be resolved", "c" in resolvedDocument)
        assertTrue(
            "Deep chain: no fragment names should remain",
            !resolvedDocument.contains("FragmentA") &&
                !resolvedDocument.contains("FragmentB") &&
                !resolvedDocument.contains("FragmentC")
        )
    }

    // ---------------------------------------------------------------------------
    // Sibling nested fragments (one parent references multiple children)
    // ---------------------------------------------------------------------------

    @Test
    fun `should resolve sibling nested fragments from a single parent`() {
        // MediaCoreFragment references MediaTitleFragment, MediaImageFragment,
        // and FuzzyDateFragment. All sibling children should be resolved.
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetSiblings",
                type = OperationType.QUERY,
                document = "query GetSiblings { media { ...ParentFragment } }",
                sourceFile = "sibling.graphql",
            ),
        )

        val fragments = mapOf(
            "ParentFragment" to GraphQLFragmentInfo(
                name = "ParentFragment",
                document = "fragment ParentFragment on Media { id title { ...TitleFragment } cover { ...ImageFragment } date { ...DateFragment } }",
            ),
            "TitleFragment" to GraphQLFragmentInfo(
                name = "TitleFragment",
                document = "fragment TitleFragment on Title { romaji english }",
            ),
            "ImageFragment" to GraphQLFragmentInfo(
                name = "ImageFragment",
                document = "fragment ImageFragment on Image { url width height }",
            ),
            "DateFragment" to GraphQLFragmentInfo(
                name = "DateFragment",
                document = "fragment DateFragment on Date { year month day }",
            ),
        )

        val resolved = resolver.resolve(operations, fragments)

        val resolvedDocument = resolved.single().document

        // Parent fields
        assertTrue("Siblings: 'id' should be resolved", "id" in resolvedDocument)

        // All sibling child fields
        assertTrue("Siblings: 'romaji' should be resolved", "romaji" in resolvedDocument)
        assertTrue("Siblings: 'english' should be resolved", "english" in resolvedDocument)
        assertTrue("Siblings: 'url' should be resolved", "url" in resolvedDocument)
        assertTrue("Siblings: 'width' should be resolved", "width" in resolvedDocument)
        assertTrue("Siblings: 'height' should be resolved", "height" in resolvedDocument)
        assertTrue("Siblings: 'year' should be resolved", "year" in resolvedDocument)
        assertTrue("Siblings: 'month' should be resolved", "month" in resolvedDocument)
        assertTrue("Siblings: 'day' should be resolved", "day" in resolvedDocument)

        // No fragment names should remain
        assertTrue(
            "Siblings: no fragment names should appear",
            !resolvedDocument.contains("ParentFragment") &&
                !resolvedDocument.contains("TitleFragment") &&
                !resolvedDocument.contains("ImageFragment") &&
                !resolvedDocument.contains("DateFragment")
        )
    }

    // ---------------------------------------------------------------------------
    // Shared nested fragment (two parents reference same child)
    // ---------------------------------------------------------------------------

    @Test
    fun `should resolve shared nested fragment across multiple operations`() {
        // Two operations, each references a different parent fragment, but both
        // parents transitively share the same child fragment. Both operations
        // should resolve the child independently.
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetA",
                type = OperationType.QUERY,
                document = "query GetA { item { ...ParentA } }",
                sourceFile = "shared.graphql",
            ),
            GraphQLOperationInfo(
                name = "GetB",
                type = OperationType.QUERY,
                document = "query GetB { item { ...ParentB } }",
                sourceFile = "shared.graphql",
            ),
        )

        val fragments = mapOf(
            "ParentA" to GraphQLFragmentInfo(
                name = "ParentA",
                document = "fragment ParentA on Item { name ...SharedChild }",
            ),
            "ParentB" to GraphQLFragmentInfo(
                name = "ParentB",
                document = "fragment ParentB on Item { age ...SharedChild }",
            ),
            "SharedChild" to GraphQLFragmentInfo(
                name = "SharedChild",
                document = "fragment SharedChild on Item { commonField }",
            ),
        )

        val resolved = resolver.resolve(operations, fragments)

        val documentA = resolved.find { it.name == "GetA" }!!.document
        val documentB = resolved.find { it.name == "GetB" }!!.document

        // Both operations should have the child field
        assertTrue(
            "GetA: should contain 'commonField' from shared child",
            "commonField" in documentA
        )
        assertTrue(
            "GetB: should contain 'commonField' from shared child",
            "commonField" in documentB
        )

        // Parent-specific fields should be in each
        assertTrue("GetA: should contain 'name'", "name" in documentA)
        assertTrue("GetB: should contain 'age'", "age" in documentB)

        // No fragment names should remain in either
        assertTrue(
            "GetA: no fragment names should appear",
            !documentA.contains("ParentA") && !documentA.contains("SharedChild")
        )
        assertTrue(
            "GetB: no fragment names should appear",
            !documentB.contains("ParentB") && !documentB.contains("SharedChild")
        )
    }

    // ---------------------------------------------------------------------------
    // Missing nested fragment — operation -> Parent -> MissingChild
    // ---------------------------------------------------------------------------

    @Test
    fun `should throw when a nested fragment is not defined`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetData",
                type = OperationType.QUERY,
                document = "query GetData { data { ...ParentFragment } }",
                sourceFile = "missing.graphql",
            ),
        )

        val fragments = mapOf(
            "ParentFragment" to GraphQLFragmentInfo(
                name = "ParentFragment",
                document = "fragment ParentFragment on Data { id ...MissingChild }",
            ),
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(operations, fragments)
        }

        assertTrue(
            "Error message should mention the missing nested fragment name",
            exception.message!!.contains("MissingChild")
        )
    }

    // ---------------------------------------------------------------------------
    // Cyclic fragments through transitive references
    // ---------------------------------------------------------------------------

    @Test
    fun `should detect cycles through transitive fragment references`() {
        // FragmentA -> FragmentB -> FragmentC -> FragmentA creates a cycle
        // at depth. Operation only references FragmentA directly.
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetCyclic",
                type = OperationType.QUERY,
                document = "query GetCyclic { item { ...FragmentA } }",
                sourceFile = "cycle.graphql",
            ),
        )

        val fragments = mapOf(
            "FragmentA" to GraphQLFragmentInfo(
                name = "FragmentA",
                document = "fragment FragmentA on Item { a ...FragmentB }",
            ),
            "FragmentB" to GraphQLFragmentInfo(
                name = "FragmentB",
                document = "fragment FragmentB on Item { b ...FragmentC }",
            ),
            "FragmentC" to GraphQLFragmentInfo(
                name = "FragmentC",
                document = "fragment FragmentC on Item { c ...FragmentA }",
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
    // Fragment with variables in transitive chain
    // ---------------------------------------------------------------------------

    @Test
    fun `should preserve variable references through transitive fragment inlining`() {
        // Operation -> FragmentA(uses $var) -> FragmentB(also uses $var)
        // After inlining, the variable references should still be present
        // in the document string.
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetVarChain",
                type = OperationType.QUERY,
                document = "query GetVarChain(\$format: String) { item { ...FragmentA } }",
                sourceFile = "vars.graphql",
            ),
        )

        val fragments = mapOf(
            "FragmentA" to GraphQLFragmentInfo(
                name = "FragmentA",
                document = "fragment FragmentA on Item { name ...FragmentB }",
            ),
            "FragmentB" to GraphQLFragmentInfo(
                name = "FragmentB",
                document = "fragment FragmentB on Item { formatted(format: \$format) }",
                variableUsages = listOf("format"),
            ),
        )

        val resolved = resolver.resolve(operations, fragments)

        val resolvedDocument = resolved.single().document

        // Variables should be preserved in the resolved document
        assertTrue(
            "Variable reference '\$format' should be preserved",
            resolvedDocument.contains("\$format")
        )
        assertTrue(
            "Variable declaration '\$format: String' should be preserved",
            resolvedDocument.contains("format: String")
        )
        // All fields should be resolved
        assertTrue("Variable chain: 'name' should be resolved", "name" in resolvedDocument)
        assertTrue(
            "Variable chain: 'formatted' should be resolved",
            "formatted" in resolvedDocument
        )
    }

    // ---------------------------------------------------------------------------
    // Multiple operations with no fragment overlaps (sanity check)
    // ---------------------------------------------------------------------------

    @Test
    fun `should independently resolve fragments for unrelated operations`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetX",
                type = OperationType.QUERY,
                document = "query GetX { x { ...XFrag } }",
                sourceFile = "multi.graphql",
            ),
            GraphQLOperationInfo(
                name = "GetY",
                type = OperationType.QUERY,
                document = "query GetY { y { ...YFrag } }",
                sourceFile = "multi.graphql",
            ),
        )

        val fragments = mapOf(
            "XFrag" to GraphQLFragmentInfo(
                name = "XFrag",
                document = "fragment XFrag on X { xfield ...XChild }",
            ),
            "XChild" to GraphQLFragmentInfo(
                name = "XChild",
                document = "fragment XChild on X { xchild }",
            ),
            "YFrag" to GraphQLFragmentInfo(
                name = "YFrag",
                document = "fragment YFrag on Y { yfield ...YChild }",
            ),
            "YChild" to GraphQLFragmentInfo(
                name = "YChild",
                document = "fragment YChild on Y { ychild }",
            ),
        )

        val resolved = resolver.resolve(operations, fragments)

        val docX = resolved.find { it.name == "GetX" }!!.document
        val docY = resolved.find { it.name == "GetY" }!!.document

        // X operation has X fields but not Y fields
        assertTrue("GetX: 'xfield' should be resolved", "xfield" in docX)
        assertTrue("GetX: 'xchild' should be resolved", "xchild" in docX)
        assertTrue("GetX: 'yfield' should NOT appear", "yfield" !in docX)

        // Y operation has Y fields but not X fields
        assertTrue("GetY: 'yfield' should be resolved", "yfield" in docY)
        assertTrue("GetY: 'ychild' should be resolved", "ychild" in docY)
        assertTrue("GetY: 'xfield' should NOT appear", "xfield" !in docY)
    }

    // ---------------------------------------------------------------------------
    // Operation with no fragment spreads, fragments available (edge case)
    // ---------------------------------------------------------------------------

    @Test
    fun `should not modify operation when it has no fragment spreads even if fragments exist`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetPlain",
                type = OperationType.QUERY,
                document = "query GetPlain { item { id name } }",
                sourceFile = "plain.graphql",
            ),
        )

        val fragments = mapOf(
            "UnusedFragment" to GraphQLFragmentInfo(
                name = "UnusedFragment",
                document = "fragment UnusedFragment on Item { extra }",
            ),
        )

        val resolved = resolver.resolve(operations, fragments)

        assertEquals(
            "Document with no fragment spreads should remain unchanged",
            operations.single().document,
            resolved.single().document,
        )
    }
}
