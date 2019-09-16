package io.github.wax911.library.annotation.processor.fragment

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * The test methods in this class will run three times using the JUnit parameterized test mechanism. Whether we are
 * dealing with an operation of type: "query", "mutation", or "subscription", the tests should pass.
 */
@RunWith(Parameterized::class)
class RegexFragmentAnalyzerTest(private val operation: Operation) {
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
        %s Some%s {
          some%s {
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
    fun `Given an operation with all fragments defined, When analyze fragments, Then analysis matches expectation`() {
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
    fun `Given an operation with 1 of 3 fragments defined, When analyze fragments, Then analysis matches expectation`() {
        val expected = setOf(
            FragmentAnalysis(fragmentA, true),
            FragmentAnalysis(fragmentB, false),
            FragmentAnalysis(fragmentC, false)
        )

        val definedFragments = """
            ${createFragment(fragmentA, typeA)}
        """.trimIndent()

        assertEquals(expected, subj.analyzeFragments(createQuery(definedFragments)))
    }

    @Test
    fun `Given an operation with no fragments defined, When analyze fragments, Then analysis matches expectation`() {
        val expected = setOf(
            FragmentAnalysis(fragmentA, false),
            FragmentAnalysis(fragmentB, false),
            FragmentAnalysis(fragmentC, false)
        )

        assertEquals(expected, subj.analyzeFragments(createQuery()))
    }

    @Test
    fun `Given an operation with fragments within fragments, When analyze fragments, Then analysis matches expectation`() {
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

        assertEquals(expected, subj.analyzeFragments(createQuery(definedFragments)))
    }

    private fun createQuery(definedFragments: String = ""): String {
        return queryTemplate.format(operation.typeStr, operation.nameStr, operation.nameStr, definedFragments)
    }

    private fun createFragment(name: String, type: String, innerFragmentRef: String = ""): String {
        return fragmentTemplate.format(name, type, innerFragmentRef)
    }

    private fun createFragmentName(letter: String) = fragmentNameTemplate.format(letter)

    private fun createType(letter: String) = typeTemplate.format(letter)

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun operation() = Operation.enumeration()
    }
}