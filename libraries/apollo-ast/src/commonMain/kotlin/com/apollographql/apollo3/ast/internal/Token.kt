package com.apollographql.apollo3.ast.internal

internal sealed class Token(val line: kotlin.Int, val column: kotlin.Int, val endLine: kotlin.Int, val endColumn: kotlin.Int) {
  object StartOfFile : Token(1, 1, 1, 1) {
    override fun toString() = "SOF"
  }

  class EndOfFile(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "EOF"
  }

  class ExclamationPoint(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "!"
  }

  class Dollar(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "$"
  }

  class Ampersand(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "&"
  }

  class LeftParenthesis(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "("
  }

  class RightParenthesis(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = ")"
  }

  class Spread(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column + 3) {
    override fun toString() = "..."
  }

  class Colon(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = ":"
  }

  class Equals(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "="
  }

  class At(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "@"
  }

  class LeftBracket(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "["
  }

  class RightBracket(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "]"
  }

  class LeftBrace(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "{"
  }

  class RightBrace(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "}"
  }

  class Pipe(line: kotlin.Int, column: kotlin.Int) : Token(line, column, line, column) {
    override fun toString() = "|"
  }

  class Name(line: kotlin.Int, column: kotlin.Int, endColumn: kotlin.Int, val value: kotlin.String) : Token(line, column, line, endColumn) {
    override fun toString() = "name: $value"
  }

  class Int(line: kotlin.Int, column: kotlin.Int, endColumn: kotlin.Int, val value: kotlin.Int) : Token(line, column, line, endColumn) {
    override fun toString() = "int: $value"
  }

  class Float(line: kotlin.Int, column: kotlin.Int, endColumn: kotlin.Int, val value: Double) : Token(line, column, line, endColumn) {
    override fun toString() = "float: $value"
  }

  class String(line: kotlin.Int, column: kotlin.Int, endLine: kotlin.Int, endColumn: kotlin.Int, val value: kotlin.String) : Token(line, column, endLine, endColumn) {
    override fun toString() = "string: \"$value\""
  }
}
