package com.apollographql.apollo.ast.internal

/**
 * @param start: the 0-indexed offset in the string where the token starts (inclusive)
 * @param end: the 0-indexed offset in the string where the token ends (exclusive)
 * @param line: the 1-indexed line in the string where the token starts (inclusive)
 * @param column: the 1-indexed column in the string where the token starts (inclusive)
 */
internal sealed class Token(val start: kotlin.Int, val end: kotlin.Int, val line: kotlin.Int, val column: kotlin.Int) {
  object StartOfFile : Token(0, 0, 1, 1) {
    override fun toString() = "SOF"
  }

  class EndOfFile(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start, line, column) {
    override fun toString() = "EOF"
  }

  class ExclamationPoint(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "!"
  }

  class QuestionMark(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "?"
  }

  class Dollar(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "$"
  }

  class Ampersand(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "&"
  }

  class LeftParenthesis(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "("
  }

  class RightParenthesis(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = ")"
  }

  class Spread(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 3, line, column ) {
    override fun toString() = "..."
  }

  class Colon(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = ":"
  }

  class Equals(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "="
  }

  class At(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "@"
  }

  class LeftBracket(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "["
  }

  class RightBracket(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "]"
  }

  class LeftBrace(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "{"
  }

  class RightBrace(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "}"
  }

  class Pipe(start: kotlin.Int, line: kotlin.Int, column: kotlin.Int) : Token(start, start + 1, line, column) {
    override fun toString() = "|"
  }

  class Name(start: kotlin.Int, end: kotlin.Int, line: kotlin.Int, column: kotlin.Int, val value: kotlin.String) : Token(start, end, line, column) {
    override fun toString() = "name: $value"
  }

  class Int(start: kotlin.Int, end: kotlin.Int, line: kotlin.Int, column: kotlin.Int, val value: kotlin.String) : Token(start, end, line, column) {
    override fun toString() = "int: $value"
  }

  class Float(start: kotlin.Int, end: kotlin.Int, line: kotlin.Int, column: kotlin.Int, val value: kotlin.String) : Token(start, end, line, column) {
    override fun toString() = "float: $value"
  }

  class String(start: kotlin.Int, end: kotlin.Int, line: kotlin.Int, column: kotlin.Int, val value: kotlin.String) : Token(start, end, line, column) {
    override fun toString() = "string: \"$value\""
  }
}
