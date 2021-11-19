package com.apollographql.apollo3.ast

import okio.Buffer
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File

fun GQLNode.toUtf8(sink: BufferedSink, indent: String = "  ") {
  val writer = SDLWriter(sink, indent)
  writer.write(this)
}

fun GQLNode.toUtf8(file: File, indent: String = "  ") = file.outputStream().sink().buffer().use {
  toUtf8(it, indent)
}

fun GQLNode.toUtf8(indent: String = "  "): String {
  val buffer = Buffer()
  toUtf8(buffer, indent)
  return buffer.readUtf8()
}

