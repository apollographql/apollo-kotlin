package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.builtinDefinitions
import com.apollographql.apollo3.ast.internal.Lexer
import com.apollographql.apollo3.ast.internal.LexerException
import com.apollographql.apollo3.ast.internal.Token
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Test ported from graphql-js
 * https://github.com/graphql/graphql-js/blob/8724e22f9e89224dd2669d6fc0c4b7fe34c2c8dd/src/language/__tests__/lexer-test.ts
 */
class LexerTest {
  private fun scanFirst(string: String): Token {
    return Lexer(Buffer().writeUtf8(string)).run {
      nextToken()
      nextToken()
    }
  }

  private fun scanSecond(string: String): Token {
    return Lexer(Buffer().writeUtf8(string)).run {
      nextToken()
      nextToken()
      nextToken()
    }
  }

  private fun Token.assertName(value: String) {
    assertIs<Token.Name>(this)
    assertEquals(value, this.value)
  }

  private fun Token.assertString(value: String) {
    assertIs<Token.String>(this)
    assertEquals(value, this.value)
  }

  @Test
  fun ignoresBOMHeader() {
    scanFirst("\uFEFF foo").apply {
      assertName("foo")
    }
  }

  @Test
  fun tracksLineBreaks() {
    scanFirst("foo").apply {
      assertName("foo")
      assertEquals(1, line)
      assertEquals(1, column)
    }
    scanFirst("\nfoo").apply {
      assertName("foo")
      assertEquals(2, line)
      assertEquals(1, column)
    }
    scanFirst("\n\rfoo").apply {
      assertName("foo")
      assertEquals(3, line)
      assertEquals(1, column)
    }
    scanFirst("\r\r\n\nfoo").apply {
      assertName("foo")
      assertEquals(4, line)
      assertEquals(1, column)
    }
    scanFirst("\n\n\r\rfoo").apply {
      assertName("foo")
      assertEquals(5, line)
      assertEquals(1, column)
    }
  }

  @Test
  fun tracksLineAndColumn() {
    scanFirst("\n \r\n \r  foo\n").apply {
      assertName("foo")
      assertEquals(4, line)
      assertEquals(3, column)
    }
  }

  @Test
  fun skipsWhitespaceAndComments() {
    scanFirst(
        """

              foo


              """
    ).apply {
      assertName("foo")
    }

    scanFirst("\t\tfoo\t\t").apply {
      assertName("foo")
    }

    scanFirst("""
      #comment
      foo#comment
    """).apply {
      assertName("foo")
    }

    scanFirst(",,,foo,,,").apply {
      assertName("foo")
    }
  }

  @Test
  fun lexesBlockString() {
    builtinDefinitions()
  }

  @Test
  fun lexesStrings() {
    scanFirst("\"\"").apply {
      assertString("")
    }
    scanFirst("\"simple\"").apply {
      assertString("simple")
    }
    scanFirst("\" white space \"").apply {
      assertString(" white space ")
    }
    scanFirst("\"quote \\\"\"").apply {
      assertString("quote \"")
    }
    scanFirst("\"escaped \\n\\r\\b\\t\\f\"").apply {
      assertString("escaped \n\r\b\t\u000c")
    }
    scanFirst("\"slashes \\\\ \\/\"").apply {
      assertString("slashes \\ /")
    }
    // 😀is 0x1f600 or \uD83D\uDE00 surrogate pair
    scanFirst("\"unescaped unicode outside BMP 😀\"").apply {
      assertString("unescaped unicode outside BMP 😀")
    }
    // 􏿿 is 0x10FFFF or \uDBFF\uDFFF surrogate pair
    scanFirst("\"unescaped maximal unicode outside BMP 􏿿\"").apply {
      assertString("unescaped maximal unicode outside BMP \uDBFF\uDFFF") //
    }
    scanFirst("\"unicode \\u1234\\u5678\\u90AB\\uCDEF\"").apply {
      assertString("unicode \u1234\u5678\u90AB\uCDEF")
    }
    scanFirst("\"unicode \\u{1234}\\u{5678}\\u{90AB}\\u{CDEF}\"").apply {
      assertString("unicode \u1234\u5678\u90AB\uCDEF")
    }
    scanFirst("\"string with unicode escape outside BMP \\u{1F600}\"").apply {
      assertString("string with unicode escape outside BMP 😀")
    }
    scanFirst("\"string with minimal unicode escape \\u{0}\"").apply {
      assertString("string with minimal unicode escape \u0000")
    }
    scanFirst("\"string with maximal unicode escape \\u{10FFFF}\"").apply {
      assertString("string with maximal unicode escape \uDBFF\uDFFF")
    }
    scanFirst("\"string with maximal minimal unicode escape \\u{0000000}\"").apply {
      assertString("string with maximal minimal unicode escape \u0000")
    }
    scanFirst("\"string with unicode surrogate pair escape \\uD83D\\uDE00\"").apply {
      assertString("string with unicode surrogate pair escape 😀")
    }
    scanFirst("\"string with minimal surrogate pair escape \\uD800\\uDC00\"").apply {
      assertString("string with minimal surrogate pair escape 𐀀")
    }
    scanFirst("\"string with maximal surrogate pair escape \\uDBFF\\uDFFF\"").apply {
      assertString("string with maximal surrogate pair escape \uDBFF\uDFFF")
    }
  }
}