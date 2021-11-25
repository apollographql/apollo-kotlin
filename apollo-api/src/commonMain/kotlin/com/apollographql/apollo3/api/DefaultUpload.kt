package com.apollographql.apollo3.api

import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import okio.use

/**
 * An [Upload] that writes data from the provided content
 *
 * If the content is a [bufferedSource], the [DefaultUpload] will close it once uploaded
 * If the content is a [byteString], the [DefaultUpload] can be reused
 */
class DefaultUpload internal constructor(
    private val bufferedSource: BufferedSource?,
    private val byteString: ByteString?,
    override val contentType: String,
    override val contentLength: Long,
    override val fileName: String?,
) : Upload {
  override fun writeTo(sink: BufferedSink) {
    if (bufferedSource != null) {
      bufferedSource.use {
        sink.writeAll(it)
      }
    } else if (byteString != null) {
      sink.write(byteString)
    } else {
      error("No upload content found")
    }
  }

  class Builder {
    private var bufferedSource: BufferedSource? = null
    private var byteString: ByteString? = null
    private var contentType: String? = null
    private var contentLength: Long = -1
    private var fileName: String? = null

    fun content(content: BufferedSource): Builder = apply {
      this.bufferedSource = content
    }

    fun content(content: String): Builder = apply {
      this.byteString = content.encodeUtf8()
      this.contentLength = content.length.toLong()
    }

    fun content(byteString: ByteString): Builder = apply {
      this.byteString = byteString
      this.contentLength = byteString.size.toLong()
    }

    fun content(byteArray: ByteArray): Builder = apply {
      this.byteString = byteArray.toByteString()
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
          bufferedSource,
          byteString,
          contentType ?: "application/octet-stream",
          contentLength,
          fileName
      )
    }
  }
}