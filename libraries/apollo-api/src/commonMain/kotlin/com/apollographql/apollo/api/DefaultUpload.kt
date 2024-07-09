@file:JvmMultifileClass
@file:JvmName("DefaultUploadKt")
package com.apollographql.apollo.api

import okio.BufferedSink
import okio.ByteString
import okio.utf8Size
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * A default [Upload] that can upload from a wide variety of content
 */
class DefaultUpload internal constructor(
    private val writeTo: (BufferedSink) -> Unit,
    override val contentType: String,
    override val contentLength: Long,
    override val fileName: String?,
) : Upload {
  override fun writeTo(sink: BufferedSink) {
    writeTo.invoke(sink)
  }

  fun newBuilder(): Builder {
    val builder = Builder()
        .content(writeTo)
        .contentType(contentType)
        .contentLength(contentLength)

    if (fileName != null) {
      builder.fileName(fileName)
    }
    return builder
  }

  class Builder {
    private var writeTo: ((BufferedSink) -> Unit)? = null
    private var contentType: String? = null
    private var contentLength: Long = -1
    private var fileName: String? = null

    fun content(writeTo: (BufferedSink) -> Unit): Builder = apply {
      check(this.writeTo == null) { "content() can only be called once" }
      this.writeTo = writeTo
    }

    fun content(content: String): Builder = apply {
      check(writeTo == null) { "content() can only be called once" }
      this.writeTo = { sink ->
        sink.writeUtf8(content)
      }
      contentLength = content.utf8Size()
    }

    fun content(byteString: ByteString): Builder = apply {
      check(writeTo == null) { "content() can only be called once" }
      this.writeTo = { sink ->
        sink.write(byteString)
      }
      this.contentLength = byteString.size.toLong()
    }

    fun content(byteArray: ByteArray): Builder = apply {
      check(writeTo == null) { "content() can only be called once" }
      this.writeTo = { sink ->
        sink.write(byteArray)
      }
      this.contentLength = byteArray.size.toLong()
    }

    fun contentType(contentType: String): Builder = apply {
      this.contentType = contentType
    }

    fun contentLength(contentLength: Long): Builder = apply {
      this.contentLength = contentLength
    }

    fun fileName(fileName: String): Builder = apply {
      this.fileName = fileName
    }

    fun build(): DefaultUpload {
      return DefaultUpload(
          writeTo ?: error("DefaultUpload content is missing"),
          contentType ?: "application/octet-stream",
          contentLength,
          fileName
      )
    }
  }
}

