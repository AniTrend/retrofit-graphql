package io.github.wax911.library.annotation.processor.fragment

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FragmentPatcherTest {
    private val defaultExtension = ".graphql"
    private val mockFragmentAnalyzer = mockk<FragmentAnalyzer>()

    private val graphFile = "SomeQuery.graphql"
    private val graphContent = "%s: actual content of this string doesn't matter for tests in this class"
    private val fragmentA = "fragmentA"
    private val fragmentB = "fragmentB"
    private val fragmentC = "fragmentC"

    private val subj = FragmentPatcher(defaultExtension, mockFragmentAnalyzer)

    /**
     * Tests this scenario:
     *
     * query SomeQuery {
     *   someObjectA {
     *     ...fragmentA
     *   }
     *   someObjectB {
     *     ...fragmentB
     *   }
     * }
     *
     * fragment fragmentA on SomeObjectA {
     *   id
     * }
     *
     * fragment fragmentB on SomeObjectB {
     *   id
     * }
     *
     * # (All fragment definitions live in the query file)
     */
    @Test
    fun `Given no missing fragment definitions, When include missing fragments, Then return empty string`() {
        val analysis = analysis(mapOf(fragmentA to true, fragmentB to true))
        val graphFiles = mapOf(graphFile to fakeContent(graphFile).first())

        every { mockFragmentAnalyzer.analyzeFragments(any()) }.returns(analysis)

        assertEquals("", subj.includeMissingFragments(graphFile, graphContent, graphFiles))
    }

    /**
     * Tests this scenario:
     *
     * query SomeQuery {
     *   someObjectA {
     *     ...fragmentA
     *   }
     * }
     *
     * # (fragmentA not defined with query, but was found in the Fragment folder)
     */
    @Test
    fun `Given a missing fragment definition, When include missing fragments, Then return missing contents`() {
        val analysis = analysis(mapOf(fragmentA to false))

        val (fragmentAKey) = file(fragmentA)

        val (queryContent, fragmentAContent) = fakeContent(graphContent, fragmentA)

        val graphFiles = mapOf(graphFile to queryContent, fragmentAKey to fragmentAContent)

        every { mockFragmentAnalyzer.analyzeFragments(any()) }.returns(emptySet())
        every { mockFragmentAnalyzer.analyzeFragments(queryContent) }.returns(analysis)

        assertTrue(subj.includeMissingFragments(graphFile, queryContent, graphFiles).contains(fragmentAContent))
    }

    /**
     * Tests this scenario:
     *
     * query SomeQuery {
     *   someObjectA {
     *     ...fragmentA
     *   }
     *   someObjectB {
     *     ...fragmentB
     *   }
     *   someObjectC {
     *     ...fragmentC
     *   }
     * }
     *
     * fragment fragmentC on SomeObjectC {
     *   id
     * }
     *
     * # (fragmentA and fragmentB not defined with query, but fragmentC is. fragmentA and fragmentB are found in the
     * #  Fragment folder)
     */
    @Test
    fun `Given multiple missing fragment definitions, When include missing fragments, Then return all missing contents`() {
        val analysis = analysis(mapOf(fragmentA to false, fragmentB to false, fragmentC to true))

        val (fragmentAKey, fragmentBKey, fragmentCKey) = file(fragmentA, fragmentB, fragmentC)

        val (queryContent, fragmentAContent, fragmentBContent, fragmentCContent) =
            fakeContent(graphFile, fragmentA, fragmentB, fragmentC)

        val graphFiles = mapOf(
            graphFile to queryContent,
            fragmentAKey to fragmentAContent,
            fragmentBKey to fragmentBContent,
            fragmentCKey to fragmentCContent
        )

        every { mockFragmentAnalyzer.analyzeFragments(any()) }.returns(emptySet())
        every { mockFragmentAnalyzer.analyzeFragments(queryContent) }.returns(analysis)

        val result = subj.includeMissingFragments(graphFile, queryContent, graphFiles)

        assertTrue(result.contains(fragmentAContent))
        assertTrue(result.contains(fragmentBContent))
        assertFalse(result.contains(fragmentCContent))
    }

    /**
     * Tests this scenario:
     *
     * query SomeQuery {
     *   someObjectA {
     *     ...fragmentA
     *   }
     * }
     *
     * # (fragmentA not defined with query, and also does not exist in the Fragment folder)
     */
    @Test
    fun `Given a fragment missing and not in map, When include missing fragments, Then return nothing`() {
        val analysis = analysis(mapOf(fragmentA to false))

        val (queryContent) = fakeContent(graphContent)

        val graphFiles = emptyMap<String, String>()

        every { mockFragmentAnalyzer.analyzeFragments(any()) }.returns(analysis)

        assertTrue(subj.includeMissingFragments(graphFile, queryContent, graphFiles).isEmpty())
    }

    /**
     * Tests this scenario:
     *
     * query SomeQuery {
     *   someObjectA {
     *     ...fragmentA
     *   }
     * }
     *
     * fragment fragmentA on SomeObjectA {
     *   id
     *   someObjectB {
     *     ...fragmentB
     *   }
     * }
     *
     * # ----------------------------
     * # Fragment/fragmentB.graphql
     * fragment fragmentB on SomeObjectB {
     *   id
     *   someObjectC {
     *     ...fragmentC
     *   }
     * }
     *
     * # ----------------------------
     * # Fragment/fragmentC.graphql
     * fragment fragmentC on SomeObjectC {
     *   id
     * }
     *
     * # (fragmentA is defined with the query. fragmentA references fragmentB which is NOT defined with the query, but
     * #  is in the Fragment folder. fragmentB references fragmentC, which is also in the Fragment folder.)
     */
    @Test
    fun `Given missing fragments and recursive references, When include missing fragments, Then return all missing contents`() {
        val queryAnalysis = analysis(mapOf(fragmentA to true, fragmentB to false))
        val fragmentBAnalysis = analysis(mapOf(fragmentC to false))

        val (fragmentBKey, fragmentCKey) = file(fragmentB, fragmentC)

        val (queryContent, fragmentBContent, fragmentCContent) =
            fakeContent(graphFile, fragmentA, fragmentB, fragmentC)

        val graphFiles = mapOf(
            graphFile to queryContent,
            fragmentBKey to fragmentBContent,
            fragmentCKey to fragmentCContent
        )

        every { mockFragmentAnalyzer.analyzeFragments(any()) }.returns(emptySet())
        every { mockFragmentAnalyzer.analyzeFragments(queryContent) }.returns(queryAnalysis)
        every { mockFragmentAnalyzer.analyzeFragments(fragmentBContent) }.returns(fragmentBAnalysis)

        val result = subj.includeMissingFragments(graphFile, queryContent, graphFiles)

        assertTrue(result.contains(fragmentBContent))
        assertTrue(result.contains(fragmentCContent))
    }

    /**
     * Tests this scenario:
     *
     * query SomeQuery {
     *   someObjectA {
     *     ...fragmentA
     *   }
     * }
     *
     * fragment fragmentA on SomeObjectA {
     *   id
     *   someObjectC {
     *     ...fragmentC
     *   }
     *   someObjectB {
     *     ...fragmentB
     *   }
     * }
     *
     * # ----------------------------
     * # Fragment/fragmentB.graphql
     * fragment fragmentB on SomeObjectB {
     *   id
     *   someObjectC {
     *     ...fragmentC
     *   }
     * }
     *
     * # ----------------------------
     * # Fragment/fragmentC.graphql
     * fragment fragmentC on SomeObjectC {
     *   id
     * }
     *
     * # (Both fragmentA and fragmentB reference fragmentC. fragmentB and fragmentC are externally defined, in the
     * #  Fragment folder. When the patch is created, fragmentB and fragmentC definition should only be added once to
     * #  the final patch).
     */
    @Test
    fun `Given a fragment referenced multiple times, When include missing fragments, Then do not duplicate in patch`() {
        val queryAnalysis = analysis(mapOf(fragmentA to true, fragmentB to false, fragmentC to false))
        val fragmentBAnalysis = analysis(mapOf(fragmentC to false))

        val (fragmentBKey, fragmentCKey) = file(fragmentB, fragmentC)

        val (queryContent, fragmentBContent, fragmentCContent) =
            fakeContent(graphFile, fragmentA, fragmentB, fragmentC)

        val graphFiles = mapOf(
            graphFile to queryContent,
            fragmentBKey to fragmentBContent,
            fragmentCKey to fragmentCContent
        )

        every { mockFragmentAnalyzer.analyzeFragments(any()) }.returns(emptySet())
        every { mockFragmentAnalyzer.analyzeFragments(queryContent) }.returns(queryAnalysis)
        every { mockFragmentAnalyzer.analyzeFragments(fragmentBContent) }.returns(fragmentBAnalysis)

        val result = subj.includeMissingFragments(graphFile, queryContent, graphFiles)

        assertEquals(1, fragmentBContent.toRegex(RegexOption.LITERAL).findAll(result).count())
        assertEquals(1, fragmentCContent.toRegex(RegexOption.LITERAL).findAll(result).count())
    }

    private fun file(vararg name: String) = name.map { "$it$defaultExtension" }.toList()

    private fun analysis(map: Map<String, Boolean>) = map.map { FragmentAnalysis(it.key, it.value) }.toSet()

    private fun fakeContent(vararg identifier: String) = identifier.map { graphContent.format(it) }.toList()
}