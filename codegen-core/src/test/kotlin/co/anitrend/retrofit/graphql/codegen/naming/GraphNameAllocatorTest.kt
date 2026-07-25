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

package co.anitrend.retrofit.graphql.codegen.naming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphNameAllocatorTest {

    // ---- Hard keywords ----

    @Test
    fun `private keyword escapes to privateValue with wireName preserved`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("private")
        assertEquals("privateValue", result.kotlinName)
        assertEquals("private", result.wireName)
    }

    @Test
    fun `object keyword escapes to objectValue`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("object")
        assertEquals("objectValue", result.kotlinName)
        assertEquals("object", result.wireName)
    }

    @Test
    fun `when keyword escapes to whenValue`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("when")
        assertEquals("whenValue", result.kotlinName)
        assertEquals("when", result.wireName)
    }

    @Test
    fun `is keyword escapes to isValue`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("is")
        assertEquals("isValue", result.kotlinName)
        assertEquals("is", result.wireName)
    }

    @Test
    fun `class keyword escapes to classValue`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("class")
        assertEquals("classValue", result.kotlinName)
        assertEquals("class", result.wireName)
    }

    @Test
    fun `fun keyword escapes to funValue`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("fun")
        assertEquals("funValue", result.kotlinName)
        assertEquals("fun", result.wireName)
    }

    @Test
    fun `all hard keywords are escaped`() {
        for (keyword in NamePolicy.HARD_KEYWORDS) {
            val result = GraphNameAllocator().allocatePropertyName(keyword)
            assertNotEquals(
                "Keyword '$keyword' should not produce kotlinName equal to wireName",
                result.wireName,
                result.kotlinName,
            )
            assertEquals("Wire name must be preserved for '$keyword'", keyword, result.wireName)
            assertTrue(
                "Keyword '$keyword' kotlinName '$result.kotlinName' should end with 'Value'",
                result.kotlinName.endsWith("Value"),
            )
        }
    }

    // ---- Soft keyword in context ----

    @Test
    fun `soft keyword like data is NOT escaped as property`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("data")
        assertEquals("data", result.kotlinName)
        assertEquals("data", result.wireName)
    }

    // ---- Collision resolution ----

    @Test
    fun `two GraphQL names normalising to same Kotlin candidate produce deterministic collision resolution`() {
        val allocator = GraphNameAllocator()

        // "class" and "classValue" both normalise to "classValue" after keyword escaping
        val first = allocator.allocatePropertyName("class")
        val second = allocator.allocatePropertyName("classValue")

        assertEquals("classValue", first.kotlinName)
        assertEquals("class", first.wireName)

        // Second should get a collision suffix
        assertNotEquals(first.kotlinName, second.kotlinName)
        assertTrue(
            "Second name should start with classValue",
            second.kotlinName.startsWith("classValue"),
        )
        assertEquals("classValue", second.wireName)
    }

    @Test
    fun `repeated allocation with same GraphQL name gets unique kotlinName`() {
        val allocator = GraphNameAllocator()

        val first = allocator.allocatePropertyName("foo")
        val second = allocator.allocatePropertyName("foo")

        assertEquals("foo", first.kotlinName)
        assertEquals("foo", first.wireName)
        assertNotEquals(first.kotlinName, second.kotlinName)
        assertTrue(second.kotlinName.startsWith("foo"))
        assertEquals("foo", second.wireName)
    }

    @Test
    fun `collision resolution is deterministic`() {
        fun allocate(): Pair<String, String> {
            val a = GraphNameAllocator()
            val first = a.allocatePropertyName("class")
            val second = a.allocatePropertyName("classValue")
            return first.kotlinName to second.kotlinName
        }

        val (f1, f2) = allocate()
        val (s1, s2) = allocate()
        assertEquals(f1, s1)
        assertEquals(f2, s2)
    }

    // ---- Leading underscore ----

    @Test
    fun `leading underscore names are preserved`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("__typename")
        assertEquals("__typename", result.kotlinName)
        assertEquals("__typename", result.wireName)
    }

    // ---- Leading digit ----

    @Test
    fun `leading digit names are prefixed with underscore`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("3dModel")
        assertEquals("_3dModel", result.kotlinName)
        assertEquals("3dModel", result.wireName)
    }

    // ---- Mixed case ----

    @Test
    fun `mixed case property name lowercases first character`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("MixedCase")
        assertEquals("mixedCase", result.kotlinName)
        assertEquals("MixedCase", result.wireName)
    }

    @Test
    fun `mixed case class name uppercases first character`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocateClassName("mixedCaseType")
        assertEquals("MixedCaseType", result.kotlinName)
        assertEquals("mixedCaseType", result.wireName)
    }

    // ---- Enum constant allocation ----

    @Test
    fun `lowercase enum value becomes uppercase kotlin constant with wire name preserved`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocateEnumConstant("open")
        assertEquals("OPEN", result.kotlinName)
        assertEquals("open", result.wireName)
    }

    @Test
    fun `already uppercase enum value is preserved`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocateEnumConstant("OPEN")
        assertEquals("OPEN", result.kotlinName)
        assertEquals("OPEN", result.wireName)
    }

    @Test
    fun `snake case enum value becomes screaming snake`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocateEnumConstant("created_at")
        assertEquals("CREATED_AT", result.kotlinName)
        assertEquals("created_at", result.wireName)
    }

    // ---- Wire name invariant ----

    @Test
    fun `wireName is never derived from kotlinName for keywords`() {
        val allocator = GraphNameAllocator()
        val keywords = listOf("private", "object", "when", "is", "class", "fun")
        for (keyword in keywords) {
            val result = allocator.allocatePropertyName(keyword)
            // The wireName should be the original keyword, not the escaped kotlinName
            assertFalse(
                "wireName '${result.wireName}' should not equal kotlinName '${result.kotlinName}' for keyword '$keyword'",
                result.wireName == result.kotlinName,
            )
            assertEquals(keyword, result.wireName)
        }
    }

    @Test
    fun `wireName is preserved even when name requires no transformation`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("login")
        assertEquals("login", result.kotlinName)
        assertEquals("login", result.wireName)
    }

    // ---- Deterministic output ----

    @Test
    fun `output is deterministic across repeated calls with fresh allocator`() {
        for (name in listOf("private", "object", "when", "class", "foo")) {
            val r1 = GraphNameAllocator().allocatePropertyName(name)
            val r2 = GraphNameAllocator().allocatePropertyName(name)
            assertEquals(
                "kotlinName mismatch for '$name'",
                r1.kotlinName,
                r2.kotlinName,
            )
            assertEquals(
                "wireName mismatch for '$name'",
                r1.wireName,
                r2.wireName,
            )
        }
    }

    // ---- Visibility modifier soft keywords (Concern 3) ----

    @Test
    fun `private soft keyword property escapes to privateValue with wireName preserved`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("private")
        assertEquals("privateValue", result.kotlinName)
        assertEquals("private", result.wireName)
    }

    @Test
    fun `public soft keyword property escapes to publicValue with wireName preserved`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("public")
        assertEquals("publicValue", result.kotlinName)
        assertEquals("public", result.wireName)
    }

    @Test
    fun `protected soft keyword property escapes to protectedValue with wireName preserved`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("protected")
        assertEquals("protectedValue", result.kotlinName)
        assertEquals("protected", result.wireName)
    }

    @Test
    fun `internal soft keyword property escapes to internalValue with wireName preserved`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocatePropertyName("internal")
        assertEquals("internalValue", result.kotlinName)
        assertEquals("internal", result.wireName)
    }

    // ---- Hard keyword enum constant (Concern 4) ----

    @Test
    fun `class hard keyword enum constant escapes to CLASSVALUE with wireName preserved`() {
        val allocator = GraphNameAllocator()
        val result = allocator.allocateEnumConstant("class")
        assertEquals("CLASSVALUE", result.kotlinName)
        assertEquals("class", result.wireName)
    }
}
