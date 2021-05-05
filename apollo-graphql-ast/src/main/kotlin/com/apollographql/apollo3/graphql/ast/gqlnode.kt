package com.apollographql.apollo3.graphql.ast

import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File

fun GQLNode.toUtf8WithIndents(): String {
  // TODO("stream the indents")
  val buffer = Buffer()
  write(buffer)
  return buffer.readUtf8().withIndents()
}

fun GQLNode.toUtf8(): String {
  val buffer = Buffer()
  write(buffer)
  return buffer.readUtf8()
}

fun GQLNode.toFile(file: File) = file.outputStream().sink().buffer().use {
  write(it)
}


private fun String.withIndents(): String {
  var indent = 0
  return lines().joinToString(separator = "\n") { line ->
    if (line.endsWith("}")) indent -= 2
    if (indent < 0) {
      // This happens if a description ends with '}'
      indent = 0
    }
    (" ".repeat(indent) + line).also {
      if (line.endsWith("{")) indent += 2
    }
  }
}
