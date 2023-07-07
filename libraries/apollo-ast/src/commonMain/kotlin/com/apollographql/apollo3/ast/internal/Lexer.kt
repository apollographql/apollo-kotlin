package com.apollographql.apollo3.ast.internal

import okio.Buffer
import okio.BufferedSource
import okio.Closeable
import okio.EOFException

/**
 * A GraphQL lexer that emits [Token]s from a [BufferedSource]
 *
 * The source must contain utf-8 content. Other encodings are not supported.
 *
 * The code is inspired by the Moshi Json reader and works at the byte level. As most of GraphQL (names, alias, etc...) is plain ASCII
 * this saves decoding utf-8 for a good chunk of the source. Because we need to track the position and line/column, we can't use the
 * same logic as Moshi and create String directly from the bytes. Instead, we need to read each code point individually.
 *
 * The [Lexer] must be closed after use
 *
 * Throws
 * - [LexerException] on malformed input
 * - [okio.IOException] on I/O error
 * - most likely [kotlin.IndexOutOfBoundsException] if trying to lex a large file
 */
internal class Lexer(val source: BufferedSource) : Closeable {
  private var position = 0
  private var line = 1
  private var lineStart = 0
  private var started = false
  private val buffer = source.buffer

  private fun readUtf8CodePointOrEof(): Int {
    return try {
      source.readUtf8CodePoint().also { position++ }
    } catch (e: EOFException) {
      -1
    }
  }

  private fun discardComment() {
    while (true) {
      val c = readUtf8CodePointOrEof()
      if (c == -1) {
        return // EOF will be caught by the main loop
      }

      when (c) {
        '\n'.code -> {
          line++
          lineStart = position
          break
        }

        '\r'.code -> {
          if (source.request(1) && buffer[0] == '\n'.code.toByte()) {
            source.skip(1)
            position++
          }
          line++
          lineStart = position
          break
        }
      }
    }
  }

  private fun Byte.isNameStart(): Boolean {
    return when (this) {
      '_'.code.toByte(),
      in 'A'.code.toByte()..'Z'.code.toByte(),
      in 'a'.code.toByte()..'z'.code.toByte(),
      -> true

      else -> false
    }
  }

  fun nextToken(): Token {
    if (!started) {
      started = true
      return Token.StartOfFile
    }

    while (source.request(1)) {
      val b = buffer[0]

      // do not consume the byte just yet, names and numbers need the first by
      if (b.isNameStart()) {
        return readName()
      }
      if (b.isDigit() || b == '-'.code.toByte()) {
        return readNumber()
      }

      // everything else can consume the byte
      val start = position
      buffer.skip(1)
      position++

      when (b) {
        // BOM https://www.unicode.org/glossary/#byte_order_mark
        0xef.toByte() -> {
          if (!source.request(2) || buffer[0] != 0xbb.toByte() || buffer[1] != 0xbf.toByte()) {
            throw LexerException("Invalid BOM", line, column(start), null)
          }
          buffer.skip(2)
          position += 1
          continue
        }

        // whitespace
        '\t'.code.toByte(),
        ' '.code.toByte(),
        ','.code.toByte(),
        -> {
          continue
        }

        '\n'.code.toByte() -> {
          line++
          lineStart = position
        }

        '\r'.code.toByte() -> {
          if (source.request(1) && buffer[0] == '\n'.code.toByte()) {
            source.skip(1)
            position++
          }
          line++
          lineStart = position
        }

        '#'.code.toByte() -> {
          discardComment()
        }

        '!'.code.toByte() -> return Token.ExclamationPoint(line, column(start))
        '$'.code.toByte() -> return Token.Dollar(line, column(start))
        '&'.code.toByte() -> return Token.Ampersand(line, column(start))
        '('.code.toByte() -> return Token.LeftParenthesis(line, column(start))
        ')'.code.toByte() -> return Token.RightParenthesis(line, column(start))
        '.'.code.toByte() -> {
          if (source.request(2) && buffer[0] == '.'.code.toByte() && buffer[1] == '.'.code.toByte()) {
            buffer.skip(2)
            position += 2
            return Token.Spread(line, column(start))
          } else {
            throw LexerException("Unfinished spread operator", line, column(start), null)
          }
        }

        ':'.code.toByte() -> return Token.Colon(line, column(start))
        '='.code.toByte() -> return Token.Equals(line, column(start))
        '@'.code.toByte() -> return Token.At(line, column(start))
        '['.code.toByte() -> return Token.LeftBracket(line, column(start))
        ']'.code.toByte() -> return Token.RightBracket(line, column(start))
        '{'.code.toByte() -> return Token.LeftBrace(line, column(start))
        '}'.code.toByte() -> return Token.RightBrace(line, column(start))
        '|'.code.toByte() -> return Token.Pipe(line, column(start))
        '"'.code.toByte() -> {
          return if (source.request(2) && buffer[0] == '"'.code.toByte() && buffer[1] == '"'.code.toByte()) {
            buffer.skip(2)
            position += 2
            readBlockString()
          } else {
            readString()
          }
        }

        else -> throw LexerException("Unexpected symbol '${b.asChar()}' (${b.toInt().and(0xff).toString(16)})", line, column(start), null)
      }
    }

    return Token.EndOfFile(line, column(position))
  }

  private fun Byte.asChar(): Char {
    return Char(toInt().and(0xffff))
  }

  // we are just after '\'
  private fun readUnicodeEscape(): Int {
    if (!source.request(1)) {
      throw LexerException("Unfinished Unicode escape", line, column(position), null)
    }

    return when (buffer[0]) {
      '{'.code.toByte() -> {
        buffer.skip(1)
        position++
        readVariableUnicodeEscape()
      }

      else -> {
        // TODO verify that 2 consecutive surrogates form a valid pair
        readFixedUnicodeEscape()
      }
    }
  }

  // we are just after '{'
  private fun readVariableUnicodeEscape(): Int {
    var i = 0
    var result = 0
    // An int32 has 8 hex digits max
    while (i < 8) {
      if (!source.request(1)) {
        throw LexerException("Unfinished Unicode escape", line, column(position), null)
      }
      val b = buffer.readByte()
      position++

      if (b == '}'.code.toByte()) {
        if (i == 0) {
          throw LexerException("Invalid Unicode escape", line, column(position), null)
        }

        // Verify that the code point is valid?
        return result
      }

      result = result.shl(4).or(b.decodeHex())
      i++
    }

    throw LexerException("Invalid Unicode escape", line, column(position), null)
  }

  private fun Byte.decodeHex(): Int {
    return when (this) {
      in 0x30..0x39 -> {
        this - 0x30
      }

      in 0x41..0x46 -> {
        this - 0x37
      }

      in 0x61..0x66 -> {
        this - 0x57
      }

      else -> throw LexerException("Invalid Unicode escape '$this", line, column(position), null)
    }
  }

  private fun Buffer.readHexDigit(): Int {
    return readByte().decodeHex().also { position++ }
  }

  private fun readFixedUnicodeEscape(): Int {
    if (!source.request(4)) {
      throw LexerException("Unfinished Unicode escape", line, column(position), null)
    }

    return (buffer.readHexDigit().shl(12))
        .or(buffer.readHexDigit().shl(8))
        .or(buffer.readHexDigit().shl(4))
        .or(buffer.readHexDigit())
  }

  private fun readEscapeCharacter(): Int {
    if (!source.request(1)) {
      throw LexerException("Unfinished escape", line, column(position), null)
    }
    val b = buffer.readByte()
    position++

    return when (b) {
      '"'.code.toByte() -> '"'.code
      '\\'.code.toByte() -> '\\'.code
      '/'.code.toByte() -> '/'.code
      'b'.code.toByte() -> '\b'.code
      'f'.code.toByte() -> '\u000C'.code
      'n'.code.toByte() -> '\n'.code
      'r'.code.toByte() -> '\r'.code
      't'.code.toByte() -> '\t'.code
      'u'.code.toByte() -> readUnicodeEscape()
      else -> throw LexerException("Invalid escape character '\\${b.asChar()}'", line, column(position), null)
    }
  }

  private fun readString(): Token {
    val builder = StringBuilder()
    val start = position - 1 // because of "

    while (true) {
      val c = readUtf8CodePointOrEof()
      if (c == -1) {
        throw LexerException("Unfinished string", line, column(position), null)
      }

      when (c) {
        '\\'.code -> builder.appendCodePointMpp(readEscapeCharacter())
        '\"'.code -> return Token.String(line, column(start), line, column(position - 1), builder.toString())
        else -> builder.appendCodePointMpp(c)
      }
    }
  }

  private fun readBlockString(): Token {
    val start = position - 3 // because of """
    val startLine = line
    val blockLines = mutableListOf<String>()
    val currentLine = StringBuilder()

    while (true) {
      val c = readUtf8CodePointOrEof()
      if (c == -1) {
        throw LexerException("Unterminated block string", line, column(position), null)
      }

      when (c) {
        '\n'.code -> {
          line++
          lineStart = position
          blockLines.add(currentLine.toString())
          currentLine.clear()
        }

        '\r'.code -> {
          if (source.request(1) && buffer[0] == '\n'.code.toByte()) {
            source.skip(1)
            position++
          }
          line++
          lineStart = position
          blockLines.add(currentLine.toString())
          currentLine.clear()
        }

        '\\'.code -> {
          if (source.request(3) &&
              buffer[0] == '\"'.code.toByte() &&
              buffer[1] == '\"'.code.toByte() &&
              buffer[2] == '\"'.code.toByte()
          ) {
            buffer.skip(3)
            position += 3
            currentLine.append("\"\"\"")
          }
        }

        '\"'.code -> {
          if (source.request(2) &&
              buffer[0] == '\"'.code.toByte() &&
              buffer[1] == '\"'.code.toByte()
          ) {
            buffer.skip(2)
            position += 2

            blockLines.add(currentLine.toString())

            return Token.String(
                startLine,
                column(start),
                line,
                column(position - 1),
                blockLines.dedentBlockStringLines().joinToString("\n")
            )
          } else {
            currentLine.appendCodePointMpp(c)
          }
        }

        else -> currentLine.appendCodePointMpp(c)
      }
    }
  }

  private fun Byte.isDigit(): Boolean {
    return when (this) {
      in '0'.code.toByte()..'9'.code.toByte() -> true
      else -> false
    }
  }

  private fun readDigits(from: Long, firstByte: Byte): Long {
    if (!firstByte.isDigit()) {
      throw LexerException("Invalid number, expected digit but got '${firstByte.asChar()}'", line, column(position + from.toInt()), null)
    }

    var i = from
    while (true) {
      if (!buffer.request(i + 1)) {
        throw LexerException("Unterminated number", line, column(position + i.toInt()), null)
      }
      if (!buffer[i].isDigit()) {
        break
      }
      i++
    }

    return i
  }

  private fun readNumber(): Token {
    val start = position
    var isFloat = false

    var b = buffer[0] // no need to request, this was done by the caller
    var i = 1L // index of the next char to check

    if (b == '-'.code.toByte()) {
      if (!buffer.request(i + 1)) {
        throw LexerException("Unterminated number", line, column(start), null)
      }
      b = buffer[i]
      i++
    }

    if (b == '0'.code.toByte()) {
      if (!buffer.request(i + 1)) {
        throw LexerException("Unterminated number", line, column(start), null)
      }
      b = buffer[i]
      i++
      if (b.isDigit()) {
        throw LexerException("Invalid number, unexpected digit after 0: '${b.asChar()}'", line, column(start), null)
      }
    } else {
      i = readDigits(i, b)
      b = buffer[i]
      i++
    }

    if (b == '.'.code.toByte()) {
      isFloat = true

      if (!buffer.request(i + 1)) {
        throw LexerException("Unterminated number", line, column(start), null)
      }
      b = buffer[i]
      i++

      i = readDigits(i, b)
      b = buffer[i]
      i++
    }

    if (b == 'e'.code.toByte() || b == 'E'.code.toByte()) {
      isFloat = true

      if (!buffer.request(i + 1)) {
        throw LexerException("Unterminated number", line, column(start), null)
      }
      b = buffer[i]
      i++

      if (b == '-'.code.toByte() || b == '+'.code.toByte()) {
        if (!buffer.request(i + 1)) {
          throw LexerException("Unterminated number", line, column(start), null)
        }
        b = buffer[i]
        i++
      }

      i = readDigits(i, b)
      b = buffer[i]
      i++
    }

    // Numbers cannot be followed by . or NameStart
    if (b == '.'.code.toByte() || b.isNameStart()) {
      throw LexerException("Invalid number, unexpected char after digit '${b.asChar()}'", line, column(start), null)
    }

    // the last byte is one byte too far, backtrack
    val asString = buffer.readUtf8(i - 1)
    position += (i - 1).toInt()

    return if (isFloat) {
      Token.Float(line, column(start), column(position - 1), asString.toDouble())
    } else {
      Token.Int(line, column(start), column(position - 1), asString.toInt())
    }
  }

  private fun Byte.isNameContinue(): Boolean {
    return when (this) {
      '_'.code.toByte(),
      in '0'.code.toByte()..'9'.code.toByte(),
      in 'A'.code.toByte()..'Z'.code.toByte(),
      in 'a'.code.toByte()..'z'.code.toByte(),
      -> true

      else -> false
    }
  }

  // Assumes Byte is > 0 (which is the case for names)
  private fun Byte.asNameChar(): Char {
    return Char(toInt())
  }

  private fun readName(): Token {
    val start = position
    val builder = StringBuilder()

    // we're guaranteed this is a name start
    builder.append(buffer[0].asNameChar())
    buffer.skip(1)
    position++

    while (source.request(1)) {
      val c = buffer[0]
      if (c.isNameContinue()) {
        builder.append(c.asNameChar())
        buffer.skip(1)
        position++
      } else {
        break
      }
    }
    return Token.Name(line = line, column = column(start), endColumn = column(position - 1), value = builder.toString())
  }

  private fun column(pos: Int): Int {
    return pos - lineStart + 1
  }

  override fun close() {
    source.close()
  }
}

internal fun List<String>.dedentBlockStringLines(): List<String> {
  var commonIndent = Int.MAX_VALUE
  var firstNonEmptyLine: Int? = null
  var lastNonEmptyLine = -1

  for (i in indices) {
    val line = get(i)
    val indent = line.leadingWhitespace()

    if (indent == line.length) {
      continue
    }

    if (firstNonEmptyLine == null) {
      firstNonEmptyLine = i
    }
    lastNonEmptyLine = i

    if (i != 0 && indent < commonIndent) {
      commonIndent = indent
    }
  }

  return mapIndexed { index, line ->
    if (index == 0) {
      line
    } else {
      line.substring(commonIndent.coerceAtMost(line.length))
    }
  }.subList(firstNonEmptyLine ?: 0, lastNonEmptyLine + 1)
}

internal fun String.leadingWhitespace(): Int {
  var i = 0
  while (i < length && get(i).isWhitespace()) {
    i++
  }

  return i
}

/**
 * Remove once we bump the Kotlin stdlib version we depend on
 */
internal fun <T> buildList(block: MutableList<T>.() -> Unit): List<T> {
  val list = mutableListOf<T>()
  list.block()
  return list
}
