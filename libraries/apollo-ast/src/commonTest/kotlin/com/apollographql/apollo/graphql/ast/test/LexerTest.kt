package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.internal.Lexer
import com.apollographql.apollo.ast.internal.LexerException
import com.apollographql.apollo.ast.internal.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Test ported from graphql-js
 * https://github.com/graphql/graphql-js/blob/8724e22f9e89224dd2669d6fc0c4b7fe34c2c8dd/src/language/__tests__/lexer-test.ts
 */
class LexerTest {
  private fun lexFirst(string: String): Token {
    return Lexer(string).run {
      nextToken()
      nextToken()
    }
  }

  private fun lexSecond(string: String): Token {
    return Lexer(string).run {
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
    lexFirst("\uFEFF foo").apply {
      assertName("foo")
    }
  }

  @Test
  fun tracksLineBreaks() {
    lexFirst("foo").apply {
      assertName("foo")
      assertEquals(1, line)
      assertEquals(1, column)
    }
    lexFirst("\nfoo").apply {
      assertName("foo")
      assertEquals(2, line)
      assertEquals(1, column)
    }
    lexFirst("\n\rfoo").apply {
      assertName("foo")
      assertEquals(3, line)
      assertEquals(1, column)
    }
    lexFirst("\r\r\n\nfoo").apply {
      assertName("foo")
      assertEquals(4, line)
      assertEquals(1, column)
    }
    lexFirst("\n\n\r\rfoo").apply {
      assertName("foo")
      assertEquals(5, line)
      assertEquals(1, column)
    }
  }

  @Test
  fun tracksLineAndColumn() {
    lexFirst("\n \r\n \r  foo\n").apply {
      assertName("foo")
      assertEquals(4, line)
      assertEquals(3, column)
    }
  }

  @Test
  fun skipsWhitespaceAndComments() {
    lexFirst(
        """

              foo


              """
    ).apply {
      assertName("foo")
    }

    lexFirst("\t\tfoo\t\t").apply {
      assertName("foo")
    }

    lexFirst("""
      #comment
      foo#comment
    """).apply {
      assertName("foo")
    }

    lexFirst(",,,foo,,,").apply {
      assertName("foo")
    }
  }

  @Test
  fun lexesStrings() {
    lexFirst("\"\"").apply {
      assertString("")
    }
    lexFirst("\"simple\"").apply {
      assertString("simple")
    }
    lexFirst("\" white space \"").apply {
      assertString(" white space ")
    }
    lexFirst("\"quote \\\"\"").apply {
      assertString("quote \"")
    }
    lexFirst("\"escaped \\n\\r\\b\\t\\f\"").apply {
      assertString("escaped \n\r\b\t\u000c")
    }
    lexFirst("\"slashes \\\\ \\/\"").apply {
      assertString("slashes \\ /")
    }
    // 😀is 0x1f600 or \uD83D\uDE00 surrogate pair
    lexFirst("\"unescaped unicode outside BMP 😀\"").apply {
      assertString("unescaped unicode outside BMP 😀")
    }
    // 􏿿 is 0x10FFFF or \uDBFF\uDFFF surrogate pair
    lexFirst("\"unescaped maximal unicode outside BMP 􏿿\"").apply {
      assertString("unescaped maximal unicode outside BMP \uDBFF\uDFFF") //
    }
    lexFirst("\"unicode \\u1234\\u5678\\u90AB\\uCDEF\"").apply {
      assertString("unicode \u1234\u5678\u90AB\uCDEF")
    }
    lexFirst("\"unicode \\u{1234}\\u{5678}\\u{90AB}\\u{CDEF}\"").apply {
      assertString("unicode \u1234\u5678\u90AB\uCDEF")
    }
    lexFirst("\"string with unicode escape outside BMP \\u{1F600}\"").apply {
      assertString("string with unicode escape outside BMP 😀")
    }
    lexFirst("\"string with minimal unicode escape \\u{0}\"").apply {
      assertString("string with minimal unicode escape \u0000")
    }
    lexFirst("\"string with maximal unicode escape \\u{10FFFF}\"").apply {
      assertString("string with maximal unicode escape \uDBFF\uDFFF")
    }
    lexFirst("\"string with maximal minimal unicode escape \\u{0000000}\"").apply {
      assertString("string with maximal minimal unicode escape \u0000")
    }
    lexFirst("\"string with unicode surrogate pair escape \\uD83D\\uDE00\"").apply {
      assertString("string with unicode surrogate pair escape 😀")
    }
    lexFirst("\"string with minimal surrogate pair escape \\uD800\\uDC00\"").apply {
      assertString("string with minimal surrogate pair escape 𐀀")
    }
    lexFirst("\"string with maximal surrogate pair escape \\uDBFF\\uDFFF\"").apply {
      assertString("string with maximal surrogate pair escape \uDBFF\uDFFF")
    }
  }

  private fun expectLexerException(string: String, block: LexerException.() -> Unit) {
    try {
      Lexer(string).let {
        while (it.nextToken() !is Token.EndOfFile) {
        }
      }
    } catch (e: LexerException) {
      e.block()
      return
    }
    error("an exception was expected")
  }

  @Test
  fun lexerReportsUsefulStringErrors() {
    expectLexerException("\"") {
      assertEquals("Unterminated string", message)
      assertEquals(1, line)
      assertEquals(2, column)
    }

    expectLexerException("\"\"\"") {
      assertEquals("Unterminated block string", message)
      assertEquals(1, line)
      assertEquals(4, column)
    }

    expectLexerException("\"\"\"\"") {
      assertEquals("Unterminated block string", message)
      assertEquals(1, line)
      assertEquals(5, column)
    }

    expectLexerException("\"no end quote") {
      assertEquals("Unterminated string", message)
      assertEquals(1, line)
      assertEquals(14, column)
    }

    expectLexerException("'single quotes'") {
      assertEquals("Unexpected symbol ''' (0x27)", message)
      assertEquals(1, line)
      assertEquals(1, column)
    }

    // TODO
    //expectLexerException("\"bad surrogate \uDEAD\"") {
    //  assertEquals("Unexpected symbol ''' (0x27)", message)
    //  assertEquals(1, line)
    //  assertEquals(16, column)
    //}
    //expectLexerException("\"bad high surrogate pair \uDEAD\uDEAD\"") {
    //  assertEquals("Unexpected symbol ''' (0x27)", message)
    //  assertEquals(1, line)
    //  assertEquals(16, column)
    //}
    //expectLexerException("\"bad low surrogate pair \uD800\uD800\"") {
    //  assertEquals("Unexpected symbol ''' (0x27)", message)
    //  assertEquals(1, line)
    //  assertEquals(16, column)
    //}

    expectLexerException("\"multi\nline\"") {
      assertEquals("Unterminated string", message)
      assertEquals(1, line)
      assertEquals(7, column)
    }

    expectLexerException("\"multi\rline\"") {
      assertEquals("Unterminated string", message)
      assertEquals(1, line)
      assertEquals(7, column)
    }

    expectLexerException("\"bad \\z esc\"") {
      assertEquals("Invalid escape character '\\z'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }

    expectLexerException("\"bad \\x esc\"") {
      assertEquals("Invalid escape character '\\x'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }

    expectLexerException("\"bad \\u1 esc\"") {
      assertEquals("Invalid Unicode escape '\\u1 '", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("\"bad \\u0XX1 esc\"") {
      assertEquals("Invalid Unicode escape '\\u0X'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("\"bad \\uXXXX esc\"") {
      assertEquals("Invalid Unicode escape '\\uX'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("\"bad \\uFXXX esc\"") {
      assertEquals("Invalid Unicode escape '\\uFX'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("\"bad \\uXXXF esc\"") {
      assertEquals("Invalid Unicode escape '\\uX'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("\"bad \\u{} esc\"") {
      assertEquals("Invalid Unicode escape '\\u{}'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("\"bad \\u{FXXX} esc\"") {
      assertEquals("Invalid Unicode escape '\\u{FX'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("\"bad \\u{FFFF esc\"") {
      assertEquals("Invalid Unicode escape '\\u{FFFF '", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("\"bad \\u{FFFF\"") {
      assertEquals("Invalid Unicode escape '\\u{FFFF\"'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("\"too high \\u{110000} esc\"") {
      assertEquals("Invalid Unicode escape '\\u{110000}'", message)
      assertEquals(1, line)
      assertEquals(11, column)
    }
    expectLexerException("\"way too high \\u{12345678} esc\"") {
      assertEquals("Invalid Unicode escape '\\u{12345678}'", message)
      assertEquals(1, line)
      assertEquals(15, column)
    }

    expectLexerException("\"too long \\u{000000000} esc\"") {
      assertEquals("Invalid Unicode escape '\\u{000000000'", message)
      assertEquals(1, line)
      assertEquals(11, column)
    }

    expectLexerException("\"bad surrogate \\uDEAD esc\"") {
      assertEquals("Invalid Unicode escape '\\uDEAD'", message)
      assertEquals(1, line)
      assertEquals(16, column)
    }

    expectLexerException("\"bad surrogate \\u{DEAD} esc\"") {
      assertEquals("Invalid Unicode escape '\\u{DEAD}'", message)
      assertEquals(1, line)
      assertEquals(16, column)
    }
    expectLexerException("\"cannot use braces for surrogate pair \\u{D83D}\\u{DE00} esc\"") {
      assertEquals("Invalid Unicode escape '\\u{D83D}'", message)
      assertEquals(1, line)
      assertEquals(39, column)
    }
    expectLexerException("\"bad high surrogate pair \\uDEAD\\uDEAD esc\"") {
      assertEquals("Invalid Unicode escape '\\uDEAD'", message)
      assertEquals(1, line)
      assertEquals(26, column)
    }
    expectLexerException("\"bad low surrogate pair \\uD800\\uD800 esc\"") {
      assertEquals("Invalid Unicode escape '\\uD800\\uD800'", message)
      assertEquals(1, line)
      assertEquals(25, column)
    }
    expectLexerException("\"cannot escape half a pair \uD83D\\uDE00 esc\"") {
      assertEquals("Invalid Unicode escape '\\uDE00'", message)
      assertEquals(1, line)
      assertEquals(29, column)
    }
    expectLexerException("\"cannot escape half a pair \\uD83D\uDE00 esc\"") {
      assertEquals("Invalid Unicode escape '\\uD83D'", message)
      assertEquals(1, line)
      assertEquals(28, column)
    }
    expectLexerException("\"bad \\uD83D\\not an escape\"") {
      assertEquals("Invalid Unicode escape '\\uD83D'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
  }

  @Test
  fun lexesBlockStrings() {
    lexFirst("\"\"\"\"\"\"").apply {
      assertString("")
    }
    lexFirst("\"\"\"simple\"\"\"").apply {
      assertString("simple")
    }
    lexFirst("\"\"\" white space \"\"\"").apply {
      assertString(" white space ")
    }
    lexFirst("\"\"\"contains \" quote\"\"\"").apply {
      assertString("contains \" quote")
    }
    lexFirst("\"\"\"contains \\\"\"\" triple quote\"\"\"").apply {
      assertString("contains \"\"\" triple quote")
    }
    lexFirst("\"\"\"multi\nline\"\"\"").apply {
      assertString("multi\nline")
    }
    lexFirst("\"\"\"multi\rline\r\nnormalized\"\"\"").apply {
      assertString("multi\nline\nnormalized")
    }
    lexFirst("\"\"\"unescaped \\n\\r\\b\\t\\f\\u1234\"\"\"").apply {
      assertString("unescaped \\n\\r\\b\\t\\f\\u1234")
    }
    lexFirst("\"\"\"unescaped unicode outside BMP \\u{1f600}\"\"\"").apply {
      assertString("unescaped unicode outside BMP \\u{1f600}")
    }
    lexFirst("\"\"\"slashes \\\\ \\/\"\"\"").apply {
      assertString("slashes \\\\ \\/")
    }
    lexFirst("""""${'"'}

        spans
          multiple
            lines

        ""${'"'}""").apply {
      assertString("spans\n  multiple\n    lines")
    }
  }

  @Test
  fun advanceLineAfterLexingMultilineBlockString() {
    lexSecond("""""${'"'}

        spans
          multiple
            lines

        ""${'"'} second_token""").apply {
      assertName("second_token")
      assertEquals(7, line)
      assertEquals(13, column)
    }
  }

  @Test
  fun lexReportsUsefulBlockStringErrors() {
    expectLexerException("\"\"\""){
      assertEquals("Unterminated block string", message)
      assertEquals(1, line)
      assertEquals(4, column)
    }
    expectLexerException("\"\"\"no end quote"){
      assertEquals("Unterminated block string", message)
      assertEquals(1, line)
      assertEquals(16, column)
    }

    // TODO
//    expectLexerException("\"\"\"contains invalid surrogate \uDEAD\"\"\""){
//      assertEquals("Invalid character", message)
//      assertEquals(1, line)
//      assertEquals(16, column)
//    }
  }

  @Test
  fun lexesNumbers() {
    lexFirst("4").apply {
      assertIs<Token.Int>(this)
      assertEquals("4", value)
    }
    lexFirst("4.123").apply {
      assertIs<Token.Float>(this)
      assertEquals("4.123", value)
    }
    lexFirst("-4").apply {
      assertIs<Token.Int>(this)
      assertEquals("-4", value)
    }
    lexFirst("9").apply {
      assertIs<Token.Int>(this)
      assertEquals("9", value)
    }
    lexFirst("0").apply {
      assertIs<Token.Int>(this)
      assertEquals("0", value)
    }
    lexFirst("-4.123").apply {
      assertIs<Token.Float>(this)
      assertEquals("-4.123", value)
    }
    lexFirst("0.123").apply {
      assertIs<Token.Float>(this)
      assertEquals("0.123", value)
    }
    lexFirst("123e4").apply {
      assertIs<Token.Float>(this)
      assertEquals("123e4", value)
    }
    lexFirst("123E4").apply {
      assertIs<Token.Float>(this)
      assertEquals("123E4", value)
    }
    lexFirst("123e-4").apply {
      assertIs<Token.Float>(this)
      assertEquals("123e-4", value)
    }
    lexFirst("123e+4").apply {
      assertIs<Token.Float>(this)
      assertEquals("123e+4", value)
    }
    lexFirst("-1.123e4").apply {
      assertIs<Token.Float>(this)
      assertEquals("-1.123e4", value)
    }
    lexFirst("-1.123E4").apply {
      assertIs<Token.Float>(this)
      assertEquals("-1.123E4", value)
    }

    lexFirst("-1.123e-4").apply {
      assertIs<Token.Float>(this)
      assertEquals("-1.123e-4", value)
    }

    lexFirst("-1.123e+4").apply {
      assertIs<Token.Float>(this)
      assertEquals("-1.123e+4", value)
    }
    lexFirst("-1.123e4567").apply {
      assertIs<Token.Float>(this)
      assertEquals("-1.123e4567", value)
    }
    lexFirst("31536000000").apply {
      assertIs<Token.Int>(this)
      assertEquals("31536000000", value)
    }
    lexFirst("31536000000.0").apply {
      assertIs<Token.Float>(this)
      assertEquals("31536000000.0", value)
    }
  }

  @Test
  fun lexReportsUsefulNumberErrors() {
    expectLexerException("00") {
      assertEquals("Invalid number, unexpected digit after 0: '0'", message)
      assertEquals(1, line)
      assertEquals(2, column)
    }
    expectLexerException("01") {
      assertEquals("Invalid number, unexpected digit after 0: '1'", message)
      assertEquals(1, line)
      assertEquals(2, column)
    }
    expectLexerException("01.23") {
      assertEquals("Invalid number, unexpected digit after 0: '1'", message)
      assertEquals(1, line)
      assertEquals(2, column)
    }
    expectLexerException("+1") {
      assertEquals("Unexpected symbol '+' (0x2b)", message)
      assertEquals(1, line)
      assertEquals(1, column)
    }
    expectLexerException("1.") {
      assertEquals("Unterminated number", message)
      assertEquals(1, line)
      assertEquals(1, column)
    }
    expectLexerException("1e") {
      assertEquals("Unterminated number", message)
      assertEquals(1, line)
      assertEquals(1, column)
    }
    expectLexerException("1E") {
      assertEquals("Unterminated number", message)
      assertEquals(1, line)
      assertEquals(1, column)
    }
    expectLexerException("1.e1") {
      assertEquals("Invalid number, expected digit but got 'e'", message)
      assertEquals(1, line)
      assertEquals(3, column)
    }
    expectLexerException(".123") {
      assertEquals("Unterminated spread operator", message)
      assertEquals(1, line)
      assertEquals(1, column)
    }
    expectLexerException("1.A") {
      assertEquals("Invalid number, expected digit but got 'A'", message)
      assertEquals(1, line)
      assertEquals(3, column)
    }
    expectLexerException("-A") {
      assertEquals("Invalid number, expected digit but got 'A'", message)
      assertEquals(1, line)
      assertEquals(2, column)
    }
    expectLexerException("1.0e") {
      assertEquals("Unterminated number", message)
      assertEquals(1, line)
      assertEquals(1, column)
    }
    expectLexerException("1.0eA") {
      assertEquals("Invalid number, expected digit but got 'A'", message)
      assertEquals(1, line)
      assertEquals(5, column)
    }
    expectLexerException("1.0e\"") {
      assertEquals("Invalid number, expected digit but got '\"'", message)
      assertEquals(1, line)
      assertEquals(5, column)
    }
    expectLexerException("1.2e3e") {
      assertEquals("Invalid number, expected digit but got 'e'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("1.2e3.4") {
      assertEquals("Invalid number, expected digit but got '.'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
    expectLexerException("1.23.4") {
      assertEquals("Invalid number, expected digit but got '.'", message)
      assertEquals(1, line)
      assertEquals(5, column)
    }
    expectLexerException("0xF1") {
      assertEquals("Invalid number, expected digit but got 'x'", message)
      assertEquals(1, line)
      assertEquals(2, column)
    }
    expectLexerException("0b10") {
      assertEquals("Invalid number, expected digit but got 'b'", message)
      assertEquals(1, line)
      assertEquals(2, column)
    }
    expectLexerException("123abc") {
      assertEquals("Invalid number, expected digit but got 'a'", message)
      assertEquals(1, line)
      assertEquals(4, column)
    }
    expectLexerException("1_234") {
      assertEquals("Invalid number, expected digit but got '_'", message)
      assertEquals(1, line)
      assertEquals(2, column)
    }
    expectLexerException("1\u00DF") {
      assertEquals("Unexpected symbol 'ß' (0xdf)", message)
      assertEquals(1, line)
      assertEquals(2, column)
    }
    expectLexerException("1.23f") {
      assertEquals("Invalid number, expected digit but got 'f'", message)
      assertEquals(1, line)
      assertEquals(5, column)
    }
    expectLexerException("1.234_5") {
      assertEquals("Invalid number, expected digit but got '_'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
  }

  @Test
  fun blockStringColumn() {
    val currentVariantSdl = """
	""${'"'}
	Directive description
	""${'"'}
	directive @someDirective(
	    ""${'"'}Argument desedcription""${'"'}
		arg1: String @deprecated(reason: "directive on argument")
	) repeatable on ENUM | SCHEMA
	type Query { fieldA: String }
""".trimIndent()

    Lexer(currentVariantSdl).apply {
      nextToken()
      assertEquals(1, nextToken().column)
    }
  }
}