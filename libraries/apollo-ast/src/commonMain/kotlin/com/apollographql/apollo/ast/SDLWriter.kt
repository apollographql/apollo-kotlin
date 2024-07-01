package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloInternal
import okio.BufferedSink
import okio.Closeable

/**
 * A [SDLWriter] writes utf8 text to the given sink and supports [indent]/[unindent]
 */
@ApolloInternal
open class SDLWriter(
    private val sink: BufferedSink,
    private val indent: String,
): Closeable {
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
      sink.writeUtf8CodePoint(it.code)
    }
  }

  open fun write(gqlNode: GQLNode) {
    gqlNode.writeInternal(this)
  }

  override fun close() {
    return sink.close()
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