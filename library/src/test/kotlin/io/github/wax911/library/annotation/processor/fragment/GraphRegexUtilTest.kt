package io.github.wax911.library.annotation.processor.fragment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * The test methods in this class will run three times using the JUnit parameterized test mechanism. Whether we are
 * dealing with an operation of type: "query", "mutation", or "subscription", the tests should pass.
 */
@RunWith(Parameterized::class)
class GraphRegexUtilTest(private val operation: Operation) {
    private val fragmentNameA = "someObjectAFragment"
    private val fragmentNameB = "someObjectBFragment"
    private val fragmentNameC = "someObjectCFragment"
    private val fragmentNameD = "_someObjectDFragment_123"

    // All valid formatting.
    private val validFormatQueryType = """
        %s Some%s {
          some%s {
            id
            name
            someObjectA {
              # Well formatted
              ...$fragmentNameA
            }
            someObjectB {
              # With a space after
              ... $fragmentNameB
            }
            someObjectC {
              # With multiple spaces after
              ...   $fragmentNameC
            }
            someObjectD {
              # Non alpha chars in name
              ... $fragmentNameD
            }
          }
        }

        # Well formatted
        fragment $fragmentNameA on SomeObjectA {
          id
          name
        }

        # With extra spaces between definition
        fragment   $fragmentNameB   on   SomeObjectB {
          id
          name
        }

        # Broken across multiple lines.
        fragment
        $fragmentNameC
        on
        SomeObjectC {
          id
          name
        }

        # Non alpha chars in name
        fragment $fragmentNameD on SomeObjectD {
          id
          name
        }
    """.trimIndent()

    // All bad/incorrect formatting.
    private val invalidFormatQueryType = """
        %s Some%s {
          some%s {
            id
            name
            someObjectA {
              # Missing a dot (.)
              ..$fragmentNameA
            }
            someObjectB {
              # No dots
              $fragmentNameB
            }
            someObjectC {
              # Invalid fragment name character (!)
              $fragmentNameC!
            }
          }
        }

        # Missing the "fragment" keyword
        $fragmentNameA on SomeObjectA {
          id
          name
        }

        # Missing the "on" keyword
        fragment $fragmentNameB SomeObjectB {
          id
          name
        }

        # Invalid fragment name character (*)
        fragment $fragmentNameC* on SomeObjectC {
          id
          name
        }

        # No spaces between keywords
        fragment${fragmentNameD}onSomeObjectD {
          id
          name
        }
    """.trimIndent()

    // Some good formatting / some bad.
    private val mixedFormatQueryType = """
        %s Some%s {
          some%s {
            id
            name
            someObjectA {
              # Well formatted
              ...$fragmentNameA
            }
            someObjectB {
              # With a space after
              ... $fragmentNameB
            }
            someObjectC {
              # No dots
              $fragmentNameC
            }
            someObjectD {
              # Invalid fragment name character (!)
              $fragmentNameD!
            }
          }
        }

        # Well formatted
        fragment $fragmentNameA on SomeObjectA {
          id
          name
        }

        # With extra spaces between definition
        fragment   $fragmentNameB   on   SomeObjectB {
          id
          name
        }

        # Invalid fragment name character (*)
        fragment $fragmentNameC* on SomeObjectC {
          id
          name
        }

        # No spaces between keywords
        fragment${fragmentNameD}onSomeObjectD {
          id
          name
        }
    """.trimIndent()

    private val subj = GraphRegexUtil

    @Test
    fun `Given all valid formatting in operation, When find fragment references, Then find all`() {
        val expected = setOf(fragmentNameA, fragmentNameB, fragmentNameC, fragmentNameD)
        assertEquals(expected, subj.findFragmentReferences(getQuery(validFormatQueryType)))
    }

    @Test
    fun `Given all invalid formatting in operation, When find fragment references, Then find nothing`() {
        val expected = setOf<String>()
        assertEquals(expected, subj.findFragmentReferences(getQuery(invalidFormatQueryType)))
    }

    @Test
    fun `Given mixed good and bad formatting in operation, When find fragment references, Then find only the good`() {
        val expected = setOf(fragmentNameA, fragmentNameB)
        assertEquals(expected, subj.findFragmentReferences(getQuery(mixedFormatQueryType)))
    }

    @Test
    fun `Given all valid formatting in operation, When find fragment definitions, Then find all`() {
        val expected = setOf(fragmentNameA, fragmentNameB, fragmentNameC, fragmentNameD)
        assertEquals(expected, subj.findFragmentDefinitions(getQuery(validFormatQueryType)))
    }

    @Test
    fun `Given all invalid formatting in operation, When find fragment definitions, Then find nothing`() {
        val expected = setOf<String>()
        assertEquals(expected, subj.findFragmentDefinitions(getQuery(invalidFormatQueryType)))
    }

    @Test
    fun `Given mixed good and bad formatting in operation, When find fragment definitions, Then find only the good`() {
        val expected = setOf(fragmentNameA, fragmentNameB)
        assertEquals(expected, subj.findFragmentDefinitions(getQuery(mixedFormatQueryType)))
    }

    @Test
    fun `Given a valid operation, When contains a query type, Then return true`() {
        println(getQuery(validFormatQueryType))
        assertTrue(subj.containsAnOperation(getQuery(validFormatQueryType)))
    }

    @Test
    fun `Given not a valid operation, When contains a query type, Then return false`() {
        assertFalse(subj.containsAnOperation(fragmentNameA))
    }

    private fun getQuery(queryToFormat: String): String {
        return queryToFormat.format(operation.typeStr, operation.nameStr, operation.nameStr)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun operation() = Operation.enumeration()
    }
}
