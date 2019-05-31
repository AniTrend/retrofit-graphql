package io.github.wax911.library.annotation.processor.fragment

import org.junit.Assert.assertEquals
import org.junit.Test

class RegexFragmentAnalyzerSpec {
    private val fragmentNameTemplate = "someObject%sFragment"
    private val typeTemplate = "Object%s"
    private val fragmentA = createFragmentName("A")
    private val fragmentB = createFragmentName("B")
    private val fragmentC = createFragmentName("C")
    private val fragmentD = createFragmentName("D")
    private val typeA = createType("A")
    private val typeB = createType("B")
    private val typeC = createType("C")
    private val typeD = createType("D")

    private val fragmentTemplate = """
        fragment %s on %s {
          id
          name
          %s
        }
    """.trimIndent()

    private val queryTemplate = """
        query SomeQuery {
          someQuery {
            objectA {
              ...$fragmentA
            }
            objectB {
              ...$fragmentB
            }
            objectC {
              ...$fragmentC
            }
          }
        }

        %s
    """.trimIndent()

    private val subj = RegexFragmentAnalyzer()

    @Test
    fun `Given a query with all fragments defined, When analyze fragments, Then analysis matches expectation`() {
        val expected = setOf(
            FragmentAnalysis(fragmentA, true),
            FragmentAnalysis(fragmentB, true),
            FragmentAnalysis(fragmentC, true)
        )

        val definedFragments = """
            ${createFragment(fragmentA, typeA)}

            ${createFragment(fragmentB, typeB)}

            ${createFragment(fragmentC, typeC)}
        """.trimIndent()

        assertEquals(expected, subj.analyzeFragments(createQuery(definedFragments)))
    }

    @Test
    fun `Given a query with 1 of 3 fragments defined, When analyze fragments, Then analysis matches expectation`() {
        val expected = setOf(
            FragmentAnalysis(fragmentA, true),
            FragmentAnalysis(fragmentB, false),
            FragmentAnalysis(fragmentC, false),
            FragmentAnalysis(fragmentD, true)
        )

        val definedFragments = """
            ${createFragment(fragmentA, typeA, "...$fragmentD")}
            ${createFragment(fragmentD, typeD)}
        """.trimIndent()

        println(definedFragments)

        assertEquals(expected, subj.analyzeFragments(createQuery(definedFragments)))
    }

    @Test
    fun `Given a query with no fragments defined, When analyze fragments, Then analysis matches expectation`() {
        val expected = setOf(
            FragmentAnalysis(fragmentA, false),
            FragmentAnalysis(fragmentB, false),
            FragmentAnalysis(fragmentC, false)
        )

        assertEquals(expected, subj.analyzeFragments(createQuery()))
    }

    @Test
    fun `Given a query with fragments within fragments, When analyze fragments, Then analysis matches expectation`() {
        val expected = setOf(
            FragmentAnalysis(fragmentA, false),
            FragmentAnalysis(fragmentB, false),
            FragmentAnalysis(fragmentC, false)
        )

        assertEquals(expected, subj.analyzeFragments(createQuery()))
    }

    private fun createQuery(definedFragments: String = ""): String {
        return queryTemplate.format(definedFragments)
    }

    private fun createFragment(name: String, type: String, innerFragmentRef: String = ""): String {
        return fragmentTemplate.format(name, type, innerFragmentRef)
    }

    private fun createFragmentName(letter: String) = fragmentNameTemplate.format(letter)

    private fun createType(letter: String) = typeTemplate.format(letter)
}