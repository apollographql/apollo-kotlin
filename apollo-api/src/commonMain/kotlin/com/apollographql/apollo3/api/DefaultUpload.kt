package com.apollographql.apollo3.api

import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.buffer
import okio.use

/**
 * An [Upload] that writes data from the [bufferedSource]
 */
class DefaultUpload internal constructor(
    private val bufferedSource: BufferedSource,
    override val contentType: String,
    override val contentLength: Long,
    override val fileName: String?,
) : Upload {
  override fun writeTo(sink: BufferedSink) {
    bufferedSource.use {
      sink.writeAll(it)
    }
  }

  class Builder {
    private var bufferedSource: BufferedSource? = null
    private var contentType: String? = null
    private var contentLength: Long = -1
    private var fileName: String? = null

    fun content(content: BufferedSource): Builder = apply {
      this.bufferedSource = content
    }

    fun content(content: String): Builder = apply {
      this.bufferedSource = content.source().buffer()
    }

    fun content(byteString: ByteString): Builder = apply {
      this.bufferedSource = byteString.source().buffer()
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
          bufferedSource ?: error("No content found"),
          contentType ?: "application/octet-stream",
          contentLength,
          fileName
      )
    }
  }
}