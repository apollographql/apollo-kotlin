@file:JvmName("FileUpload")

package com.apollographql.apollo3.api

import okio.buffer
import okio.source
import java.io.File

fun DefaultUpload.Builder.content(file: File): DefaultUpload.Builder {
  return content(file.source().buffer()).contentLength(file.length())
}

@Deprecated("This is a helper function to help migrating to 3.x " +
    "and will be removed in a future version", ReplaceWith("FileUpload(File(filePath), mimetype)"))
fun create(mimetype: String, filePath: String): Upload {
  val file = File(filePath)
  return DefaultUpload.Builder()
      .content(file)
      .contentType(mimetype)
      .build()
}
