package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.builtinDefinitions
import com.apollographql.apollo3.ast.internal.Lexer
import com.apollographql.apollo3.ast.internal.LexerException
import com.apollographql.apollo3.ast.internal.Token
import okio.Buffer
import okio.use
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

  private fun expectLexerException(string: String, block: LexerException.() -> Unit) {
    try {
      Lexer(Buffer().writeUtf8(string)).use {
        while (it.nextToken() !is Token.EndOfFile) {}
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
    // expectLexerException("\"bad surrogate \\uDEAD\"") {
    //   assertEquals("Unexpected symbol ''' (0x27)", message)
    //   assertEquals(1, line)
    //   assertEquals(16, column)
    // }
    // expectLexerException("\"bad high surrogate pair \\uDEAD\\uDEAD\"") {
    //   assertEquals("Unexpected symbol ''' (0x27)", message)
    //   assertEquals(1, line)
    //   assertEquals(16, column)
    // }
    // expectLexerException("\"bad low surrogate pair \\uD800\\uD800\"") {
    //   assertEquals("Unexpected symbol ''' (0x27)", message)
    //   assertEquals(1, line)
    //   assertEquals(16, column)
    // }

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
      assertEquals("Invalid Unicode escape '\\x'", message)
      assertEquals(1, line)
      assertEquals(6, column)
    }
  }

//
//      expectSyntaxError(""bad \\u1 esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\u1 es".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError(""bad \\u0XX1 esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\u0XX1".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError(""bad \\uXXXX esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\uXXXX".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError(""bad \\uFXXX esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\uFXXX".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError(""bad \\uXXXF esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\uXXXF".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError(""bad \\u{} esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\u{}".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError(""bad \\u{FXXX} esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\u{FX".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError(""bad \\u{FFFF esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\u{FFFF ".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError(""bad \\u{FFFF"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\u{FFFF"".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError(""too high \\u{110000} esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\u{110000}".",
//        locations: [{ line: 1, column: 11 }],
//      });
//
//      expectSyntaxError(""way too high \\u{12345678} esc"").to.deep.equal({
//        message:
//        "Syntax Error: Invalid Unicode escape sequence: "\\u{12345678}".",
//        locations: [{ line: 1, column: 15 }],
//      });
//
//      expectSyntaxError(""too long \\u{000000000} esc"").to.deep.equal({
//        message:
//        "Syntax Error: Invalid Unicode escape sequence: "\\u{000000000".",
//        locations: [{ line: 1, column: 11 }],
//      });
//
//      expectSyntaxError(""bad surrogate \\uDEAD esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\uDEAD".",
//        locations: [{ line: 1, column: 16 }],
//      });
//
//      expectSyntaxError(""bad surrogate \\u{DEAD} esc"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\u{DEAD}".",
//        locations: [{ line: 1, column: 16 }],
//      });
//
//      expectSyntaxError(
//          ""cannot use braces for surrogate pair \\u{D83D}\\u{DE00} esc"",
//      ).to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\u{D83D}".",
//        locations: [{ line: 1, column: 39 }],
//      });
//
//      expectSyntaxError(
//          ""bad high surrogate pair \\uDEAD\\uDEAD esc"",
//      ).to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\uDEAD".",
//        locations: [{ line: 1, column: 26 }],
//      });
//
//      expectSyntaxError(
//          ""bad low surrogate pair \\uD800\\uD800 esc"",
//      ).to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\uD800".",
//        locations: [{ line: 1, column: 25 }],
//      });
//
//      expectSyntaxError(
//          ""cannot escape half a pair \uD83D\\uDE00 esc"",
//      ).to.deep.equal({
//        message: "Syntax Error: Invalid character within String: U+D83D.",
//        locations: [{ line: 1, column: 28 }],
//      });
//
//      expectSyntaxError(
//          ""cannot escape half a pair \\uD83D\uDE00 esc"",
//      ).to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\uD83D".",
//        locations: [{ line: 1, column: 28 }],
//      });
//
//      expectSyntaxError(""bad \\uD83D\\not an escape"").to.deep.equal({
//        message: "Syntax Error: Invalid Unicode escape sequence: "\\uD83D".",
//        locations: [{ line: 1, column: 6 }],
//      });
//    });
//
//    it("lexes block strings", () => {
//      expect(lexOne("""""""")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 6,
//        line: 1,
//        column: 1,
//        value: "",
//      });
//
//      expect(lexOne(""""simple"""")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 12,
//        line: 1,
//        column: 1,
//        value: "simple",
//      });
//
//      expect(lexOne("""" white space """")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 19,
//        line: 1,
//        column: 1,
//        value: " white space ",
//      });
//
//      expect(lexOne(""""contains " quote"""")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 22,
//        line: 1,
//        column: 1,
//        value: "contains " quote",
//      });
//
//      expect(lexOne(""""contains \\""" triple quote"""")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 32,
//        line: 1,
//        column: 1,
//        value: "contains """ triple quote",
//      });
//
//      expect(lexOne(""""multi\nline"""")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 16,
//        line: 1,
//        column: 1,
//        value: "multi\nline",
//      });
//
//      expect(lexOne(""""multi\rline\r\nnormalized"""")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 28,
//        line: 1,
//        column: 1,
//        value: "multi\nline\nnormalized",
//      });
//
//      expect(lexOne(""""unescaped \\n\\r\\b\\t\\f\\u1234"""")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 32,
//        line: 1,
//        column: 1,
//        value: "unescaped \\n\\r\\b\\t\\f\\u1234",
//      });
//
//      expect(lexOne(""""unescaped unicode outside BMP \u{1f600}"""")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 38,
//        line: 1,
//        column: 1,
//        value: "unescaped unicode outside BMP \u{1f600}",
//      });
//
//      expect(lexOne(""""slashes \\\\ \\/"""")).to.contain({
//        kind: TokenKind.BLOCK_STRING,
//        start: 0,
//        end: 19,
//        line: 1,
//        column: 1,
//        value: "slashes \\\\ \\/",
//      });
//
//      expect(
//          lexOne(`"""
//
//        spans
//          multiple
//            lines
//
//        """`),
//      ).to.contain({
//      kind: TokenKind.BLOCK_STRING,
//      start: 0,
//      end: 68,
//      line: 1,
//      column: 1,
//      value: "spans\n  multiple\n    lines",
//    });
//    });
//
//    it("advance line after lexing multiline block string", () => {
//      expect(
//          lexSecond(`"""
//
//        spans
//          multiple
//            lines
//
//        \n """ second_token`),
//      ).to.contain({
//      kind: TokenKind.NAME,
//      start: 71,
//      end: 83,
//      line: 8,
//      column: 6,
//      value: "second_token",
//    });
//
//      expect(
//          lexSecond(
//              [
//                """" \n",
//                "spans \r\n",
//                "multiple \n\r",
//                "lines \n\n",
//                """"\n second_token",
//              ].join(""),
//          ),
//      ).to.contain({
//        kind: TokenKind.NAME,
//        start: 37,
//        end: 49,
//        line: 8,
//        column: 2,
//        value: "second_token",
//      });
//    });
//
//    it("lex reports useful block string errors", () => {
//      expectSyntaxError(""""").to.deep.equal({
//        message: "Syntax Error: Unterminated string.",
//        locations: [{ line: 1, column: 4 }],
//      });
//
//      expectSyntaxError(""""no end quote").to.deep.equal({
//        message: "Syntax Error: Unterminated string.",
//        locations: [{ line: 1, column: 16 }],
//      });
//
//      expectSyntaxError(""""contains invalid surrogate \uDEAD"""").to.deep.equal({
//        message: "Syntax Error: Invalid character within String: U+DEAD.",
//        locations: [{ line: 1, column: 31 }],
//      });
//    });
//
//    it("lexes numbers", () => {
//      expect(lexOne("4")).to.contain({
//        kind: TokenKind.INT,
//        start: 0,
//        end: 1,
//        value: "4",
//      });
//
//      expect(lexOne("4.123")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 5,
//        value: "4.123",
//      });
//
//      expect(lexOne("-4")).to.contain({
//        kind: TokenKind.INT,
//        start: 0,
//        end: 2,
//        value: "-4",
//      });
//
//      expect(lexOne("9")).to.contain({
//        kind: TokenKind.INT,
//        start: 0,
//        end: 1,
//        value: "9",
//      });
//
//      expect(lexOne("0")).to.contain({
//        kind: TokenKind.INT,
//        start: 0,
//        end: 1,
//        value: "0",
//      });
//
//      expect(lexOne("-4.123")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 6,
//        value: "-4.123",
//      });
//
//      expect(lexOne("0.123")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 5,
//        value: "0.123",
//      });
//
//      expect(lexOne("123e4")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 5,
//        value: "123e4",
//      });
//
//      expect(lexOne("123E4")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 5,
//        value: "123E4",
//      });
//
//      expect(lexOne("123e-4")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 6,
//        value: "123e-4",
//      });
//
//      expect(lexOne("123e+4")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 6,
//        value: "123e+4",
//      });
//
//      expect(lexOne("-1.123e4")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 8,
//        value: "-1.123e4",
//      });
//
//      expect(lexOne("-1.123E4")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 8,
//        value: "-1.123E4",
//      });
//
//      expect(lexOne("-1.123e-4")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 9,
//        value: "-1.123e-4",
//      });
//
//      expect(lexOne("-1.123e+4")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 9,
//        value: "-1.123e+4",
//      });
//
//      expect(lexOne("-1.123e4567")).to.contain({
//        kind: TokenKind.FLOAT,
//        start: 0,
//        end: 11,
//        value: "-1.123e4567",
//      });
//    });
//
//    it("lex reports useful number errors", () => {
//      expectSyntaxError("00").to.deep.equal({
//        message: "Syntax Error: Invalid number, unexpected digit after 0: "0".",
//        locations: [{ line: 1, column: 2 }],
//      });
//
//      expectSyntaxError("01").to.deep.equal({
//        message: "Syntax Error: Invalid number, unexpected digit after 0: "1".",
//        locations: [{ line: 1, column: 2 }],
//      });
//
//      expectSyntaxError("01.23").to.deep.equal({
//        message: "Syntax Error: Invalid number, unexpected digit after 0: "1".",
//        locations: [{ line: 1, column: 2 }],
//      });
//
//      expectSyntaxError("+1").to.deep.equal({
//        message: "Syntax Error: Unexpected character: "+".",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("1.").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: <EOF>.",
//        locations: [{ line: 1, column: 3 }],
//      });
//
//      expectSyntaxError("1e").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: <EOF>.",
//        locations: [{ line: 1, column: 3 }],
//      });
//
//      expectSyntaxError("1E").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: <EOF>.",
//        locations: [{ line: 1, column: 3 }],
//      });
//
//      expectSyntaxError("1.e1").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "e".",
//        locations: [{ line: 1, column: 3 }],
//      });
//
//      expectSyntaxError(".123").to.deep.equal({
//        message: "Syntax Error: Unexpected character: ".".",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("1.A").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "A".",
//        locations: [{ line: 1, column: 3 }],
//      });
//
//      expectSyntaxError("-A").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "A".",
//        locations: [{ line: 1, column: 2 }],
//      });
//
//      expectSyntaxError("1.0e").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: <EOF>.",
//        locations: [{ line: 1, column: 5 }],
//      });
//
//      expectSyntaxError("1.0eA").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "A".",
//        locations: [{ line: 1, column: 5 }],
//      });
//
//      expectSyntaxError("1.0e"").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "\"".",
//        locations: [{ line: 1, column: 5 }],
//      });
//
//      expectSyntaxError("1.2e3e").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "e".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError("1.2e3.4").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: ".".",
//        locations: [{ line: 1, column: 6 }],
//      });
//
//      expectSyntaxError("1.23.4").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: ".".",
//        locations: [{ line: 1, column: 5 }],
//      });
//    });
//
//    it("lex does not allow name-start after a number", () => {
//      expectSyntaxError("0xF1").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "x".",
//        locations: [{ line: 1, column: 2 }],
//      });
//      expectSyntaxError("0b10").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "b".",
//        locations: [{ line: 1, column: 2 }],
//      });
//      expectSyntaxError("123abc").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "a".",
//        locations: [{ line: 1, column: 4 }],
//      });
//      expectSyntaxError("1_234").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "_".",
//        locations: [{ line: 1, column: 2 }],
//      });
//      expectSyntaxError("1\u00DF").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+00DF.",
//        locations: [{ line: 1, column: 2 }],
//      });
//      expectSyntaxError("1.23f").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "f".",
//        locations: [{ line: 1, column: 5 }],
//      });
//      expectSyntaxError("1.234_5").to.deep.equal({
//        message: "Syntax Error: Invalid number, expected digit but got: "_".",
//        locations: [{ line: 1, column: 6 }],
//      });
//    });
//
//    it("lexes punctuation", () => {
//      expect(lexOne("!")).to.contain({
//        kind: TokenKind.BANG,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("?")).to.contain({
//        kind: TokenKind.QUESTION_MARK,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("$")).to.contain({
//        kind: TokenKind.DOLLAR,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("(")).to.contain({
//        kind: TokenKind.PAREN_L,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne(")")).to.contain({
//        kind: TokenKind.PAREN_R,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("...")).to.contain({
//        kind: TokenKind.SPREAD,
//        start: 0,
//        end: 3,
//        value: undefined,
//      });
//
//      expect(lexOne(":")).to.contain({
//        kind: TokenKind.COLON,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("=")).to.contain({
//        kind: TokenKind.EQUALS,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("@")).to.contain({
//        kind: TokenKind.AT,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("[")).to.contain({
//        kind: TokenKind.BRACKET_L,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("]")).to.contain({
//        kind: TokenKind.BRACKET_R,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("{")).to.contain({
//        kind: TokenKind.BRACE_L,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("|")).to.contain({
//        kind: TokenKind.PIPE,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//
//      expect(lexOne("}")).to.contain({
//        kind: TokenKind.BRACE_R,
//        start: 0,
//        end: 1,
//        value: undefined,
//      });
//    });
//
//    it("lex reports useful unknown character error", () => {
//      expectSyntaxError("..").to.deep.equal({
//        message: "Syntax Error: Unexpected character: ".".",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("~").to.deep.equal({
//        message: "Syntax Error: Unexpected character: "~".",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\x00").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+0000.",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\b").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+0008.",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\u00AA").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+00AA.",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\u0AAA").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+0AAA.",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\u203B").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+203B.",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\u{1f600}").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+1F600.",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\uD83D\uDE00").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+1F600.",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\uD800\uDC00").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+10000.",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\uDBFF\uDFFF").to.deep.equal({
//        message: "Syntax Error: Unexpected character: U+10FFFF.",
//        locations: [{ line: 1, column: 1 }],
//      });
//
//      expectSyntaxError("\uDEAD").to.deep.equal({
//        message: "Syntax Error: Invalid character: U+DEAD.",
//        locations: [{ line: 1, column: 1 }],
//      });
//    });

}