package com.apollographql.apollo3.cache.http

import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import java.io.IOException

internal abstract class ResponseBodyCacheSink(delegate: BufferedSink?) : ForwardingSink(delegate!!) {
  private var failed = false
  @Throws(IOException::class)
  override fun write(source: Buffer, byteCount: Long) {
    if (failed) return
    try {
      super.write(source, byteCount)
    } catch (e: Exception) {
      failed = true
      onException(e)
    }
  }

  @Throws(IOException::class)
  override fun flush() {
    if (failed) return
    try {
      super.flush()
    } catch (e: Exception) {
      failed = true
      onException(e)
    }
  }

  @Throws(IOException::class)
  override fun close() {
    if (failed) return
    try {
      super.close()
    } catch (e: Exception) {
      failed = true
      onException(e)
    }
  }

  fun copyFrom(buffer: Buffer, offset: Long, bytesCount: Long) {
    if (failed) return
    try {
      val outSink = delegate as BufferedSink
      buffer.copyTo(outSink.buffer(), offset, bytesCount)
      outSink.emitCompleteSegments()
    } catch (e: Exception) {
      failed = true
      onException(e)
    }
  }

  abstract fun onException(e: Exception?)
}