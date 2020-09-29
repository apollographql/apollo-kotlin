package com.apollographql.apollo.api

import okio.BufferedSink
import okio.buffer
import okio.source
import java.io.File

fun FileUpload.Companion.create(mimetype: String, filePath: String): FileUpload {
  val file = File(filePath)
  return object : FileUpload(mimetype) {
    override fun contentLength() = file.length()
    override fun fileName() = file.name
    override fun writeTo(sink: BufferedSink) {
      sink.writeAll(file.source().buffer())
    }
  }
}
