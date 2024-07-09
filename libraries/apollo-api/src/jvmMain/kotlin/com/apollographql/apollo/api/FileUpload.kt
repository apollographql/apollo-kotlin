@file:JvmName("FileUpload")

package com.apollographql.apollo.api

import okio.buffer
import okio.source
import java.io.File

fun File.toUpload(contentType: String): DefaultUpload {
  return DefaultUpload.Builder()
      .content { sink ->
        source().buffer().use { sink.writeAll(it) }
      }
      .contentLength(length())
      .contentType(contentType)
      .fileName(name)
      .build()
}
