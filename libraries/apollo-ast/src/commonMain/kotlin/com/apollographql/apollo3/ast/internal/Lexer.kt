package com.apollographql.apollo.ast.internal

/**
 * A GraphQL lexer that emits [Token]s from a [String]
 */
internal class Lexer(val src: String) {
  /**
   *  Char position in the String (UTF-16)
   *  Because other tools use unicode position, this might create a disconnect. If that becomes an issue, we'll need to add a second
   *  property for tracking the unicode position
   */
  private var pos = 0
  private val len = src.length
  private var line = 1
  private var lineStart = 0
  private var started = false

  private fun discardComment() {
    while (true) {
      if (pos == len) {
        return // EOF will be caught by the main loop
      }
      val c = src[pos++]

      when (c) {
        '\n' -> {
          line++
          lineStart = pos
          break
        }

        '\r' -> {
          if (pos < len && src[pos] == '\n') {
            pos++
          }
          line++
          lineStart = pos
          break
        }
      }
    }
  }

  private fun Char.isNameStart(): Boolean {
    return when (this) {
      '_',
      in 'A'..'Z',
      in 'a'..'z',
      -> true

      else -> false
    }
  }

  fun nextToken(): Token {
    if (!started) {
      started = true
      return Token.StartOfFile
    }

    while (pos < len) {
      val c = src[pos]

      // do not consume the byte just yet, names and numbers need the first by
      if (c.isNameStart()) {
        return readName()
      }
      if (c.isDigit() || c == '-') {
        return readNumber()
      }

      // everything else can consume the byte
      val start = pos
      pos++

      when (c) {
        // whitespace
        0xfeff.toChar(), // BOM https://www.unicode.org/glossary/#byte_order_mark
        '\t',
        ' ',
        ',',
        -> {
          continue
        }

        '\n' -> {
          line++
          lineStart = pos
        }

        '\r' -> {
          if (pos < len && src[pos] == '\n') {
            pos++
          }
          line++
          lineStart = pos
        }

        '#' -> {
          discardComment()
        }

        '!' -> return Token.ExclamationPoint(start, line, column(start))
        '?' -> return Token.QuestionMark(start, line, column(start))
        '$' -> return Token.Dollar(start, line, column(start))
        '&' -> return Token.Ampersand(start, line, column(start))
        '(' -> return Token.LeftParenthesis(start, line, column(start))
        ')' -> return Token.RightParenthesis(start, line, column(start))
        '.' -> {
          if (pos + 1 < len && src[pos] == '.' && src[pos + 1] == '.') {
            pos += 2
            return Token.Spread(start, line, column(start))
          } else {
            throw LexerException("Unterminated spread operator", start, line, column(start), null)
          }
        }

        ':' -> return Token.Colon(start, line, column(start))
        '=' -> return Token.Equals(start, line, column(start))
        '@' -> return Token.At(start, line, column(start))
        '[' -> return Token.LeftBracket(start, line, column(start))
        ']' -> return Token.RightBracket(start, line, column(start))
        '{' -> return Token.LeftBrace(start, line, column(start))
        '}' -> return Token.RightBrace(start, line, column(start))
        '|' -> return Token.Pipe(start, line, column(start))
        '"' -> {
          return if (pos + 1 < len && src[pos] == '"' && src[pos + 1] == '"') {
            pos += 2
            readBlockString()
          } else {
            readString()
          }
        }

        else -> {
          throw LexerException("Unexpected symbol '${c}' (0x${c.code.toString(16)})", start, line, column(start), null)
        }
      }
    }

    return Token.EndOfFile(pos, line, column(pos))
  }

  // we are just after "\u"
  private fun readUnicodeEscape(): Int {
    if (pos == len) {
      throw LexerException("Unterminated Unicode escape", pos, line, column(pos), null)
    }

    when (src[pos]) {
      '{' -> {
        pos++
        return readVariableUnicodeEscape()
      }

      else -> {
        val c1 = readFixedUnicodeEscape()

        if (c1.isUnicodeScalar()) {
          return c1
        }

        val start = pos - 6

        // GraphQL allows JSON-style surrogate pair escape sequences, but only when
        // a valid pair is formed.

        if (c1.isLeadingSurrogate()) {
          if (pos + 1 < len
              && src[pos] == '\\'
              && src[pos + 1] == 'u') {
            pos += 2
            val c2 = readFixedUnicodeEscape()
            if (c2.isTrailingSurrogate()) {
              return codePoint(c1, c2)
            }
          }
        }

        throw LexerException("Invalid Unicode escape '${src.substring(start, pos)}'", start, line, column(start), null)
      }
    }
  }


  private fun Int.isLeadingSurrogate(): Boolean {
    return this in 0xd800..0xdbff
  }

  private fun Int.isTrailingSurrogate(): Boolean {
    return this in 0xdc00..0xdfff
  }

  private fun Int.isUnicodeScalar(): Boolean {
    return this in 0x0000..0xd7ff || this in 0xe000..0x10ffff
  }

  // we are just after '{'
  private fun readVariableUnicodeEscape(): Int {
    var i = 0
    var result = 0

    // An int32 has 8 hex digits max
    while (i < 9) {
      if (pos == len) {
        throw LexerException("Unterminated Unicode escape", pos, line, column(pos), null)
      }
      val c = src[pos++]

      if (c == '}') {
        if (i == 0) {
          val start = pos - i - 4
          // empty unicode escape?
          throw LexerException("Invalid Unicode escape '${src.substring(start, pos)}'", start, line, column(start), null)
        }

        if (!result.isUnicodeScalar()) {
          val start = pos - i - 4
          throw LexerException("Invalid Unicode escape '${src.substring(start, pos)}'", start, line, column(start), null)
        }

        // Verify that the code point is valid?
        return result
      }

      val h = c.decodeHex()
      if (h == -1) {
        val start = pos - i - 4
        throw LexerException("Invalid Unicode escape '${src.substring(start, pos)}'", start, line, column(start), null)
      }

      result = result.shl(4).or(h)
      i++
    }

    val start = pos - i - 3
    throw LexerException("Invalid Unicode escape '${src.substring(start, pos)}'", start, line, column(start), null)
  }

  private fun Char.decodeHex(): Int {
    return when (this.code) {
      in 0x30..0x39 -> {
        this.code - 0x30
      }

      in 0x41..0x46 -> {
        this.code - 0x37
      }

      in 0x61..0x66 -> {
        this.code - 0x57
      }

      else -> -1
    }
  }

  private fun readFixedUnicodeEscape(): Int {
    if (pos + 4 >= len) {
      throw LexerException("Unterminated Unicode escape", pos, line, column(pos), null)
    }

    var result = 0
    for (i in 0..3) {
      val h = src[pos++].decodeHex()
      if (h == -1) {
        val start = pos - i - 3
        throw LexerException("Invalid Unicode escape '${src.substring(start, pos)}'", start, line, column(start), null)
      }
      result = result.shl(4).or(h)
    }

    return result
  }

  private fun readEscapeCharacter(): Int {
    if (pos == len) {
      throw LexerException("Unterminated escape", pos, line, column(pos), null)
    }
    val c = src[pos++]

    return when (c) {
      '"' -> '"'.code
      '\\' -> '\\'.code
      '/' -> '/'.code
      'b' -> '\b'.code
      'f' -> '\u000C'.code
      'n' -> '\n'.code
      'r' -> '\r'.code
      't' -> '\t'.code
      'u' -> readUnicodeEscape()
      else -> throw LexerException("Invalid escape character '\\${c}'", pos - 2, line, column(pos - 2), null)
    }
  }

  private fun readString(): Token {
    val builder = StringBuilder()
    val start = pos - 1 // because of "

    while (true) {
      if (pos == len) {
        throw LexerException("Unterminated string", pos, line, column(pos), null)
      }
      val c = src[pos++]

      when (c) {
        '\\' -> builder.appendCodePointMpp(readEscapeCharacter())
        '\"' -> return Token.String(
            start = start,
            end = pos,
            line = line,
            column = column(start),
            value = builder.toString()
        )

        '\r', '\n' -> throw LexerException("Unterminated string", pos - 1, line, column(pos - 1), null)
        else -> {
          // TODO: we are lenient here and allow potentially invalid chars like invalid surrogate pairs
          builder.append(c)
        }
      }
    }
  }

  private fun readBlockString(): Token {
    val start = pos - 3 // because of """
    val startLine = line
    val startColumn = column(start)
    val blockLines = mutableListOf<String>()
    val currentLine = StringBuilder()

    while (true) {
      if (pos == len) {
        throw LexerException("Unterminated block string", pos, line, column(pos), null)
      }
      val c = src[pos++]

      when (c) {
        '\n' -> {
          line++
          lineStart = pos
          blockLines.add(currentLine.toString())
          currentLine.clear()
        }

        '\r' -> {
          if (pos + 1 < len && src[pos] == '\n') {
            pos++
          }
          line++
          lineStart = pos
          blockLines.add(currentLine.toString())
          currentLine.clear()
        }

        '\\' -> {
          if (pos + 2 < len &&
              src[pos] == '\"' &&
              src[pos + 1] == '\"' &&
              src[pos + 2] == '\"'
          ) {
            pos += 3
            currentLine.append("\"\"\"")
          } else {
            currentLine.append(c)
          }
        }

        '\"' -> {
          if (pos + 1 < len &&
              src[pos] == '\"' &&
              src[pos + 1] == '\"'
          ) {
            pos += 2

            blockLines.add(currentLine.toString())

            return Token.String(
                start = start,
                end = pos,
                line = startLine,
                column = startColumn,
                value = blockLines.dedentBlockStringLines().joinToString("\n")
            )
          } else {
            currentLine.append(c)
          }
        }

        else -> {
          // TODO: we are lenient here and allow potentially invalid chars like invalid surrogate pairs
          currentLine.append(c)
        }
      }
    }
  }

  private fun Char.isDigit(): Boolean {
    return when (this) {
      in '0'..'9' -> true
      else -> false
    }
  }

  private val STATE_NEGATIVE_SIGN = 1
  private val STATE_ZERO = 2
  private val STATE_DOT_EXP = 3
  private val STATE_INTEGER_DIGIT = 4
  private val STATE_FRACTIONAL_DIGIT = 5
  private val STATE_SIGN = 6
  private val STATE_EXP_DIGIT = 7
  private val STATE_EXP = 8

  private fun readNumber(): Token {
    val start = pos
    var isFloat = false

    var state = STATE_NEGATIVE_SIGN

    while (pos < len) {
      when (state) {
        STATE_NEGATIVE_SIGN -> {
          when (src[pos]) {
            '-' -> {
              pos++
              state = STATE_ZERO
            }

            else -> {
              state = STATE_ZERO
            }
          }
        }

        STATE_ZERO -> {
          var c = src[pos]
          when {
            c == '0' -> {
              pos++
              state = STATE_DOT_EXP

              if (pos == len) {
                break
              }
              c = src[pos]
              if (pos < len && c.isDigit()) {
                throw LexerException("Invalid number, unexpected digit after 0: '${c}'", pos, line, column(pos), null)
              }
            }

            c.isDigit() -> {
              pos++
              state = STATE_INTEGER_DIGIT
            }

            else -> {
              throw LexerException("Invalid number, expected digit but got '${c}'", pos, line, column(pos), null)
            }
          }
        }

        STATE_INTEGER_DIGIT -> {
          if (src[pos].isDigit()) {
            pos++
          } else {
            state = STATE_DOT_EXP
          }
        }

        STATE_DOT_EXP -> {
          when (src[pos]) {
            '.' -> {
              isFloat = true
              pos++

              if (pos == len) {
                throw LexerException("Unterminated number", start, line, column(start), null)
              }
              val c = src[pos]
              if (!c.isDigit()) {
                throw LexerException("Invalid number, expected digit but got '${c}'", pos, line, column(pos), null)
              }
              pos++
              state = STATE_FRACTIONAL_DIGIT
            }

            else -> {
              state = STATE_EXP
            }
          }
        }

        STATE_EXP -> {
          when (src[pos]) {
            'e', 'E' -> {
              isFloat = true
              pos++
              if (pos == len) {
                throw LexerException("Unterminated number", start, line, column(start), null)
              }
              state = STATE_SIGN
            }

            else -> break
          }
        }

        STATE_SIGN -> {
          var c = src[pos]
          when (c) {
            '-', '+' -> {
              pos++
              if (pos == len) {
                throw LexerException("Unterminated number", start, line, column(start), null)
              }
              c = src[pos]
              if (!c.isDigit()) {
                throw LexerException("Invalid number, expected digit but got '${c}'", pos, line, column(pos), null)
              }
              pos++
              state = STATE_EXP_DIGIT
            }

            else -> {
              if (!c.isDigit()) {
                throw LexerException("Invalid number, expected digit but got '${c}'", pos, line, column(pos), null)
              }
              pos++
              state = STATE_EXP_DIGIT
            }
          }
        }

        STATE_EXP_DIGIT -> {
          if (src[pos].isDigit()) {
            pos++
          } else {
            break
          }
        }

        STATE_FRACTIONAL_DIGIT -> {
          if (src[pos].isDigit()) {
            pos++
          } else {
            state = STATE_EXP
          }
        }
      }
    }

    // Numbers cannot be followed by . or NameStart
    if (pos < len && (src[pos] == '.' || src[pos].isNameStart())) {
      throw LexerException("Invalid number, expected digit but got '${src[pos]}'", pos, line, column(pos), null)
    }

    val asString = src.substring(start, pos)

    return if (isFloat) {
      Token.Float(
          start = start,
          end = pos,
          line = line,
          column = column(start),
          value = asString
      )
    } else {
      Token.Int(
          start = start,
          end = pos,
          line = line,
          column = column(start),
          value = asString
      )
    }
  }

  private fun Char.isNameContinue(): Boolean {
    return when (this) {
      '_',
      in '0'..'9',
      in 'A'..'Z',
      in 'a'..'z',
      -> true

      else -> false
    }
  }

  private fun readName(): Token {
    val start = pos

    // we're guaranteed this is a name start
    pos++

    while (pos < len) {
      val c = src[pos]
      if (c.isNameContinue()) {
        pos++
      } else {
        break
      }
    }
    return Token.Name(
        start = start,
        end = pos,
        line = line,
        column = column(start),
        value = src.substring(start, pos)
    )
  }

  private fun column(pos: Int): Int {
    return pos - lineStart + 1
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
