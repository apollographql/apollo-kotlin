package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import okio.utf8Size

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

    @Deprecated("This API is dangerous because the resulting upload can only be used once and can also lead to resource leaks.",
        ReplaceWith("content {sink ->\nval source = openSource()\nsource.use {sink.writeAll(it)}\n}"))
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_3)
    fun content(content: BufferedSource): Builder = apply {
      check(writeTo == null) { "content() can only be called once" }
      var consumed = false
      this.writeTo = { sink ->
        check(!consumed) { "Apollo: DefaultUpload BufferedSource body can only be read once. If you want to read it several times for logging or other purposes, either use a different kind of body or use your own `Upload` implementation." }
        content.use {
          sink.writeAll(it)
        }
        consumed = true
      }
    }

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

fun Path.toUpload(contentType: String, fileSystem: FileSystem = systemFileSystem): Upload {
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
