package com.apollographql.apollo3.ast.internal

import kotlin.math.pow

/**
 * Remove once we bump the Kotlin stdlib version we depend on
 */
internal fun <T> buildList(block: MutableList<T>.() -> Unit): List<T> {
  val list = mutableListOf<T>()
  list.block()
  return list
}

internal class Scanner(val src: String) {
  private val len = src.length
  private var pos = 0
  private var line = 1
  private var lineStart = 0
  private var started = false

  fun reset() {
    pos = 0
    line = 1
    lineStart = 0
    started = false
  }

  fun scan(): Token {
    if (!started) {
      started = true
      return Token.StartOfFile
    }
    skipWhitespaceAndComments()
    if (pos == len) {
      return Token.EndOfFile(line, column(pos))
    }
    return when (val c = src[pos]) {
      in ('A'..'Z'), '_', in ('a'..'z') -> scanName()
      '-', in '0'..'9' -> scanNumber()
      '"' -> {
        if (len > pos + 2 && src[pos + 1] == '"' && src[pos + 2] == '"') {
          return scanBlockString()
        } else {
          return scanString()
        }
      }

      '!' -> return Token.ExclamationPoint(line, column(pos)).also { pos++ }
      '$' -> return Token.Dollar(line, column(pos)).also { pos++ }
      '&' -> return Token.Ampersand(line, column(pos)).also { pos++ }
      '(' -> return Token.LeftParenthesis(line, column(pos)).also { pos++ }
      ')' -> return Token.RightParenthesis(line, column(pos)).also { pos++ }
      '.' -> {
        if (len > pos + 2 && src[pos + 1] == '.' && src[pos + 2] == '.') {
          return Token.Spread(line, column(pos)).also { pos += 3 }
        } else {
          throw ScannerException("Unfinished spread operator", line, column(pos))
        }
      }

      ':' -> return Token.Colon(line, column(pos)).also { pos++ }
      '=' -> return Token.Equals(line, column(pos)).also { pos++ }
      '@' -> return Token.At(line, column(pos)).also { pos++ }
      '[' -> return Token.LeftBracket(line, column(pos)).also { pos++ }
      ']' -> return Token.RightBracket(line, column(pos)).also { pos++ }
      '{' -> return Token.LeftBrace(line, column(pos)).also { pos++ }
      '}' -> return Token.RightBrace(line, column(pos)).also { pos++ }
      '|' -> return Token.Pipe(line, column(pos)).also { pos++ }
      else -> throw ScannerException("Unexpected symbol '${c}'", line, column(pos))
    }
  }

  private fun scanUnicode(): Char {
    val start = pos
    if (++pos == len) {
      throw ScannerException("Unfinished Unicode escape", line, column(start))
    }
    val codeString = when (src[pos]) {
      '{' -> {
        val builder = StringBuilder()
        var seen = 0;
        while (true) {
          if (++pos == len) {
            throw ScannerException("Unfinished Unicode escape", line, column(start))
          }
          val c = src[pos]
          if (c == '}') {
            break
          } else if (c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f') {
            seen++
            if (seen > 4) {
              throw ScannerException("Invalid Unicode escape, too many characters", line, column(start))
            }
            builder.append(c)
          } else {
            throw ScannerException("Invalid Unicode escape, unexpected character", line, column(start))
          }
        }
        builder.toString().also { pos++ }
      }

      in '0'..'9', in 'A'..'F', in 'a'..'f' -> {
        if (pos + 3 >= len) {
          throw ScannerException("Unfinished Unicode escape", line, column(start))
        }
        src.substring(pos, pos + 4).also { pos += 4 }
      }

      else -> throw ScannerException("Invalid Unicode escape, unexpected character", line, column(start))
    }
    return try {
      codeString.toInt(16).toChar()
    } catch (_: NumberFormatException) {
      throw ScannerException("Invalid Unicode escape", line, column(start))
    }
  }

  private fun scanString(): Token {
    val builder = StringBuilder()
    val start = pos
    pos++

    while (pos < len) {
      when (val c = src[pos]) {
        '"' -> return Token.String(line, column(start), builder.toString()).also { pos++ }

        '\\' -> {
          if (pos == len - 1) {
            throw ScannerException("Unfinished escaped", line, column(pos))
          }
          pos++
          // 	" \	/	b	f	n	r	t
          when (val e = src[pos]) {
            '"' -> builder.append('"')
            '\\' -> builder.append('\\')
            '/' -> builder.append('/')
            'b' -> builder.append('\b').also { pos++ }
            'f' -> builder.append('\u000C').also { pos++ }
            'n' -> builder.append('\n').also { pos++ }
            'r' -> builder.append('\r').also { pos++ }
            't' -> builder.append('\t').also { pos++ }
            'u' -> builder.append(scanUnicode())
            else -> throw ScannerException("Invalid escape character '$e'", line, column(pos))
          }
        }

        '\n' -> throw ScannerException("Newline in non-block string", line, column(pos))
        else -> builder.append(c).also { pos++ }
      }
    }
    throw ScannerException("Unfinished string", line, column(pos))
  }

  private fun List<String>.dedentBlockStringLines(): List<String> {
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

    return mapIndexed {  index, line ->
      if (index == 0) {
        line
      } else {
        if (line.length >= commonIndent) {
          line.substring(commonIndent)
        } else {
          line
        }
      }
    }.subList(firstNonEmptyLine ?: 0, lastNonEmptyLine + 1)
  }

  private fun String.leadingWhitespace(): Int {
    var i = 0
    while (i < length && get(i).isWhitespace()) {
      i++
    }

    return i
  }

  private fun scanBlockString(): Token {
    val start = pos
    pos += 3
    var chunkStart = pos
    var currentLine = ""
    val blockLines = mutableListOf<String>()
    val startLine = line

    while (pos < len) {
      val code = src[pos]

      if (
          len > pos + 2 &&
          code == '"' &&
          src[pos + 1] == '"' &&
          src[pos + 2] == '"'
      ) {
        currentLine += src.substring(chunkStart, pos)
        blockLines.add(currentLine)

        pos += 3
        return Token.String(
            startLine,
            column(start),
            blockLines.dedentBlockStringLines().joinToString("\n")
        )
      }

      if (
          len > pos + 3 &&
          code == '\\' &&
          src[pos + 1] == '"' &&
          src[pos + 2] == '"' &&
          src[pos + 3] == '"'
      ) {
        currentLine += src.substring(chunkStart, pos)
        chunkStart = pos + 1
        pos += 4
      }

      if (code == '\r' || code == '\n') {
        currentLine += src.substring(chunkStart, pos)
        blockLines.add(currentLine)

        if (
            len > pos + 1 &&
            code == '\r' &&
            src[pos + 1] == '\n'
        ) {
          pos += 2
        } else {
          pos++
        }

        currentLine = ""
        chunkStart = pos

        line++
        lineStart = pos

        continue
      }

      if (!code.isSurrogate()) {
        pos++
      } else if (
          len > pos + 1 &&
          code.isHighSurrogate() &&
          src[pos + 1].isLowSurrogate()
      ) {
        pos += 2
      } else {
        throw ScannerException(
            "Invalid character within String: '$code'",
            this.line + blockLines.size - 1,
            pos - lineStart
        )
      }
    }
    throw ScannerException("Unterminated string", line, column(pos));
  }

  private fun scanNumber(): Token {
    val start = pos
    var float = false
    var period = -1
    var negnum = false
    var num = 0L
    var ne = false
    var e = 0
    if (src[pos] == '-') {
      negnum = true
      pos++
    }

    while (pos < len) {
      when (val c = src[pos]) {
        in '0'..'9' -> {
          num = (num * 10 + (c - '0'))
          if (period >= 0) period++
          pos++
        }

        '.' -> {
          if (float) {
            throw ScannerException("Unexpected period", line, column(pos))
          }
          float = true
          period = 0
          pos++
        }

        'e', 'E' -> {
          float = true
          pos++
          when (src[pos]) {
            '+' -> pos++
            '-' -> {
              ne = true
              pos++
            }
          }
          while (pos < len) {
            val ec = src[pos]
            when (ec) {
              in '0'..'9' -> {
                e = e * 10 + (ec - '0')
                pos++
              }

              else -> break
            }
          }
        }

        '_', in 'A'..'Z', in 'a'..'z' ->
          throw ScannerException("A number cannot be followed by '${c}'", line, column(pos))

        else -> break
      }
    }

    if (negnum) num = -num
    return if (float) {
      val res = num.toDouble()
      if (ne) e = -e
      if (period >= 0) e -= period
      Token.Float(line, column(start), res * 10.toDouble().pow(e))
    } else {
      Token.Int(line, column(start), num.toIntExact())
    }
  }

  private fun Long.toIntExact(): Int {
    val result = toInt()
    check(result.toLong() == this) {
      "'$this' cannot be converted to Int"
    }
    return result
  }

  private fun scanName(): Token {
    val start = pos
    val n = StringBuilder()
    while (pos < len) {
      val c = src[pos]
      when (c) {
        '_', in '0'..'9', in 'A'..'Z', in 'a'..'z' -> {
          n.append(c)
          pos++
        }

        else -> break
      }
    }
    return Token.Name(line = line, column = column(start), value = n.toString())
  }

  private fun column(pos: Int): Int {
    return pos - lineStart
  }

  private fun skipWhitespaceAndComments() {
    while (pos < len) {
      val code: Char = src[pos]
      // tab | NL | CR | space | comma | BOM
      when (code) {
        '\t', '\r', ' ', ',', 0xfeff.toChar() -> pos++
        '\n' -> {
          pos++
          lineStart = pos
          line++
        }

        '#' -> {
          while (pos < len && src[pos] != '\n') {
            pos++
          }
        }

        else -> break
      }
    }
  }
}
