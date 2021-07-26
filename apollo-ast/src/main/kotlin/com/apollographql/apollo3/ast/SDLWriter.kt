package com.apollographql.apollo3.ast

import okio.BufferedSink

class SDLWriter(
    private val sink: BufferedSink,
    private val indent: String,
) {
  private var indentCount = 0
  private var bol = true

  fun indent() {
    indentCount += 1
  }

  fun unindent() {
    indentCount -= 1
  }

  fun write(string: String) {
    string.forEach {
      if (it == '\n') {
        bol = true
      } else if (bol) {
        bol = false
        repeat(indentCount) {
          sink.writeUtf8(indent)
        }
      }
      sink.writeUtf8CodePoint(it.toInt())
    }
  }
}

internal fun SDLWriter.writeDescription(description: String?) {
  if (!description.isNullOrBlank()) {
    write("\"\"\"${description.encodeToGraphQLTripleQuoted()}\"\"\"\n")
  }
}
internal fun SDLWriter.writeInlineString(string: String?) {
  if (string == null) {
    return
  }
  write("\"${string.encodeToGraphQLSingleQuoted()}\"")
}