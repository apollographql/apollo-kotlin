package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.internal.dedentBlockStringLines
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test ported from graphql-js
 * https://github.com/graphql/graphql-js/blob/8724e22f9e89224dd2669d6fc0c4b7fe34c2c8dd/src/language/__tests__/blockString-test.ts
 */
class LexerDedentTest {
  private fun runTest(input: List<String>, expected: List<String>) {
    assertEquals(expected, input.dedentBlockStringLines())
  }

  @Test
  fun emptyString() {
    runTest(emptyList(), emptyList())
  }

  @Test
  fun doesNotDedentFirstLine() {
    runTest(listOf("  a"), listOf("  a"))
    runTest(listOf(" a", "  b"), listOf(" a", "b"))
  }

  @Test
  fun removesMinimalIndentationLength() {
    runTest(listOf("", " a", "  b"), listOf("a", " b"))
    runTest(listOf("", "  a", " b"), listOf(" a", "b"))
    runTest(listOf("", "  a", " b", "c"), listOf("  a", " b", "c"))
  }

  @Test
  fun dedentBothTabAndSpaceAsSingleCharacter() {
    runTest(listOf("", "\ta", "          b"), listOf("a", "         b"))
    runTest(listOf("", "\t a", "          b"), listOf("a", "        b"))
    runTest(listOf("", " \t a", "          b"), listOf("a", "       b"))
  }

  @Test
  fun dedentDoNotTakeEmptyLinesIntoAccount() {
    runTest(listOf("a", "", " b"), listOf("a", "", "b"))
    runTest(listOf("a", " ", "  b"), listOf("a", "", "b"))
  }

  @Test
  fun removesUniformIndentationFromAString() {
    runTest(
        listOf(
            "",
            "    Hello,",
            "      World!",
            "",
            "    Yours,",
            "      GraphQL."
        ),
        listOf(
            "Hello,",
            "  World!",
            "",
            "Yours,",
            "  GraphQL."
        ),
    )
  }

  @Test
  fun removesEmptyLeadingAndTrailingLines() {
    runTest(
        listOf(
            "",
            "",
            "    Hello,",
            "      World!",
            "",
            "    Yours,",
            "      GraphQL.",
            "",
            "",
        ),
        listOf(
            "Hello,",
            "  World!",
            "",
            "Yours,",
            "  GraphQL."
        ),
    )
  }


  @Test
  fun removesBlankLeadingAndTrailingLines() {
    runTest(
        listOf(
            "  ",
            "        ",
            "    Hello,",
            "      World!",
            "",
            "    Yours,",
            "      GraphQL.",
            "        ",
            "  ",
        ),
        listOf(
            "Hello,",
            "  World!",
            "",
            "Yours,",
            "  GraphQL."
        ),
    )
  }

  @Test
  fun retainsIndentationFromFirstLine() {
    runTest(
        listOf(
            "    Hello,",
            "      World!",
            "",
            "    Yours,",
            "      GraphQL.",
        ),
        listOf(
            "    Hello,",
            "  World!",
            "",
            "Yours,",
            "  GraphQL."
        ),
    )
  }

  @Test
  fun doesNotAlterTrailingSpaces() {
    runTest(
        listOf(
            "               ",
            "    Hello,     ",
            "      World!   ",
            "               ",
            "    Yours,     ",
            "      GraphQL. ",
            "               ",
        ),
        listOf(
            "Hello,     ",
            "  World!   ",
            "           ",
            "Yours,     ",
            "  GraphQL. ",
        ),
    )
  }
}
