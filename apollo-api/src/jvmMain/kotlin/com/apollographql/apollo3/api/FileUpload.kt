package com.apollographql.apollo3.api

import okio.BufferedSink
import okio.buffer
import okio.source
import java.io.File

class FileUpload(override val contentType: String, val file: File): Upload {
  override val contentLength = file.length()
  override val fileName = file.name

  override fun writeTo(sink: BufferedSink) {
    file.inputStream().source().buffer().use {
      sink.writeAll(it)
    }
  }
}