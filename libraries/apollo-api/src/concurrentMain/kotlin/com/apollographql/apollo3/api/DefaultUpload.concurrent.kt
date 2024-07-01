@file:JvmMultifileClass
@file:JvmName("DefaultUploadKt")
package com.apollographql.apollo.api

import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer
import okio.use
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

fun Path.toUpload(contentType: String, fileSystem: FileSystem = FileSystem.SYSTEM): Upload {
  return DefaultUpload.Builder()
      .content { sink ->
        fileSystem.openReadOnly(this).use {
          sink.writeAll(it.source().buffer())
        }
      }
      .contentType(contentType)
      .contentLength(fileSystem.metadata(this).size ?: -1L)
      .build()
}
