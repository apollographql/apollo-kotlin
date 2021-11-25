package com.apollographql.apollo3.api

import okio.BufferedSink
import okio.buffer
import okio.source
import java.io.File

class FileUpload(private val file: File, override val contentType: String) : Upload {
  override val contentLength = file.length()
  override val fileName = file.name

  override fun writeTo(sink: BufferedSink) {
    file.inputStream().source().buffer().use {
      sink.writeAll(it)
    }
  }

  companion object {
    @Deprecated("This is a helper function to help migrating to 3.x " +
        "and will be removed in a future version", ReplaceWith("FileUpload(File(filePath), mimetype)"))
    fun create(mimetype: String, filePath: String): FileUpload = FileUpload(File(filePath), mimetype)
  }
}
