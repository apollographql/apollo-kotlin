package com.apollographql.apollo3.graphql.ast

object GraphQLString {
  fun decodeSingleQuoted(string: String): String {
    val writer = StringBuilder(string.length)
    var i = 0
    while (i < string.length) {
      val c = string[i]
      i += 1
      if (c != '\\') {
        writer.append(c)
        continue
      }
      val escaped = string[i]
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
          writer.append('\u000C'.toInt())
          i += 1
        }
        'n' -> {
          writer.append('\n'.toInt())
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
          val codepoint = string.substring(i + 1, i + 5).toInt(16)
          writer.appendCodePoint(codepoint)
          i += 5
        }
        else -> throw IllegalStateException("Bad escaped character: $c")
      }
    }
    return writer.toString()
  }

  fun decodeTripleQuoted(string: String): String {
    return string.replace("\\\"\"\"", "\"\"\"").trimIndent()
  }

  fun encodeSingleQuoted(string: String): String {
    return string
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
  }

  fun encodeTripleQuoted(string: String): String {
    return string
        .replace("\"\"\"", "\\\"\"\"")
        .let {
          if (it.isEmpty()) {
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
}
