package com.apollographql.apollo3.mockserver

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.plus
import kotlinx.cinterop.readBytes
import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Timeout
import platform.posix.errno

class FileDescriptorSink(val fd: Int): Sink {
  private var closed = false

  override fun write(source: Buffer, byteCount: Long) {
    require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
    check(!closed) { "closed" }

    memScoped {
      val bufSize = 8192

      var written = 0L
      var w = 0L
      var bufMax = 0L
      var buf: CArrayPointer<ByteVar>? = null
      while (written < byteCount) {
        if (buf == null || w == bufMax) {
          bufMax = minOf(bufSize.toLong(), byteCount - written)
          buf = allocArrayOf(source.readByteArray(bufMax.convert()))
          w = 0
        }

        val toWrite = bufMax - w

        val len = platform.posix.write(fd, buf.plus(w), toWrite.convert())
        if (len < 0) {
          throw IOException("Cannot write $fd (errno = $errno)")
        }
        written += len
        w += len
      }
    }
  }

  override fun close() {
    if (closed) return
    closed = true
    platform.posix.close(fd)
  }

  override fun flush() {
    // no need to flush a file descriptor I think?
  }

  override fun timeout(): Timeout = Timeout.NONE
}