package com.apollographql.apollo.api.http

import okio.BufferedSink
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

class ByteStringHttpBody(
    override val contentType: String,
    private val byteString: ByteString
): HttpBody {

  constructor(contentType: String, string: String): this(contentType, string.encodeUtf8())

  override val contentLength
    get() = byteString.size.toLong()

  override fun writeTo(bufferedSink: BufferedSink) {
    bufferedSink.write(byteString)
  }
}
