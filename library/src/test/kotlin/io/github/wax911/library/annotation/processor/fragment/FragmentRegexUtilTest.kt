package io.github.wax911.library.annotation.processor.fragment

import org.junit.Assert.assertEquals
import org.junit.Test

class FragmentRegexUtilTest {
    private val fragmentNameA = "someObjectAFragment"
    private val fragmentNameB = "someObjectBFragment"
    private val fragmentNameC = "someObjectCFragment"
    private val fragmentNameD = "_someObjectDFragment_123"

    // All valid formatting.
    private val validFormatQuery = """
        query SomeQuery {
          someQuery {
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
    private val invalidFormatQuery = """
        query SomeQuery {
          someQuery {
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
    private val mixedFormatQuery = """
        query SomeQuery {
          someQuery {
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

    private val subj = FragmentRegexUtil

    @Test
    fun `Given all valid formatting in Query, When find fragment references, Then find all`() {
        val expected = setOf(fragmentNameA, fragmentNameB, fragmentNameC, fragmentNameD)
        assertEquals(expected, subj.findFragmentReferences(validFormatQuery))
    }

    @Test
    fun `Given all invalid formatting in Query, When find fragment references, Then find nothing`() {
        val expected = setOf<String>()
        assertEquals(expected, subj.findFragmentReferences(invalidFormatQuery))
    }

    @Test
    fun `Given mixed good and bad formatting in Query, When find fragment references, Then find only the good`() {
        val expected = setOf(fragmentNameA, fragmentNameB)
        assertEquals(expected, subj.findFragmentReferences(mixedFormatQuery))
    }

    @Test
    fun `Given all valid formatting in query, When find fragment definitions, Then find all`() {
        val expected = setOf(fragmentNameA, fragmentNameB, fragmentNameC, fragmentNameD)
        assertEquals(expected, subj.findFragmentDefinitions(validFormatQuery))
    }

    @Test
    fun `Given all invalid formatting in Query, When find fragment definitions, Then find nothing`() {
        val expected = setOf<String>()
        assertEquals(expected, subj.findFragmentDefinitions(invalidFormatQuery))
    }

    @Test
    fun `Given mixed good and bad formatting in Query, When find fragment definitions, Then find only the good`() {
        val expected = setOf(fragmentNameA, fragmentNameB)
        assertEquals(expected, subj.findFragmentDefinitions(mixedFormatQuery))
    }
}