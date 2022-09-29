package com.apollographql.apollo3.ast

fun String.decodeAsGraphQLSingleQuoted(): String {
  val writer = StringBuilder(length)
  var i = 0
  while (i < length) {
    val c = get(i)
    i += 1
    if (c != '\\') {
      writer.append(c)
      continue
    }
    val escaped = get(i)
    when (escaped) {
      '"' -> {
        writer.append('"')
        i += 1
      }
      '/' -> {
        writer.append('/')
        i += 1
      }
      '\\' -> {
        writer.append('\\')
        i += 1
      }
      'b' -> {
        writer.append('\b')
        i += 1
      }
      'f' -> {
        writer.append('\u000C'.code)
        i += 1
      }
      'n' -> {
        writer.append('\n'.code)
        i += 1
      }
      'r' -> {
        writer.append('\r')
        i += 1
      }
      't' -> {
        writer.append('\t')
        i += 1
      }
      'u' -> {
        val codepoint = substring(i + 1, i + 5).toInt(16)
        writer.appendCodePoint(codepoint)
        i += 5
      }
      else -> throw IllegalStateException("Bad escaped character: $c")
    }
  }
  return writer.toString()
}

fun String.decodeAsGraphQLTripleQuoted(): String {
  val value = replace("\\\"\"\"", "\"\"\"")

  // https://spec.graphql.org/draft/#BlockStringValue()

  var lines = value.split("\n")

  var commonIndent: Int? = null
  for (line in lines.drop(1)) {
    val firstNonWhitespace = line.indexOfFirst { it != ' ' && it != '\t' }
    if (firstNonWhitespace == -1) continue
    if (commonIndent == null || firstNonWhitespace < commonIndent) {
      commonIndent = firstNonWhitespace
    }
  }

  lines = listOf(lines.first()) + lines.drop(1).map {
    if (commonIndent == null) {
      it
    } else if (it.length > commonIndent) {
      it.substring(commonIndent)
    } else {
      // The spec isn't 100% clear whether we should try to remove as many whitespace as possible or not
      it
    }
  }

  lines = lines.dropWhile {
    it.indexOfFirst { it != ' ' && it != '\t' } == -1
  }

  lines = lines.dropLastWhile {
    it.indexOfFirst { it != ' ' && it != '\t' } == -1
  }

  return lines.joinToString("\n")
}

/**
 * https://spec.graphql.org/draft/#EscapedCharacter
 */
fun String.encodeToGraphQLSingleQuoted(): String {
  return buildString {
    this@encodeToGraphQLSingleQuoted.forEach { c ->
      when (c) {
        '\"' -> append("\\\"")
        '\\' -> append("\\\\")
        '/' -> append("\\/")
        '\b' -> append("\\b")
        '\u000C' -> append("\\t")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        // TODO: handle range outside U+0020–U+FFFF
        else -> append(c)
      }
    }
  }
}

fun String.encodeToGraphQLTripleQuoted(): String {
  return this
      .replace("\"\"\"", "\\\"\"\"")
      .let {
        if (isEmpty()) {
          it
        } else {
          // Add leading and trailing lines to make it look a bit nicer (see 2.9.4 String Value)
          // Also if there's a leading or trailing '"', this disambiguates parsing
          val leading = if (it[0] != '\n') "\n" else ""
          val trailing = if (it[it.length - 1] != '\n') "\n" else ""

          "$leading$it$trailing"
        }
      }
}

