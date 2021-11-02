package com.apollographql.apollo3.mockserver

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import okio.Buffer
import okio.IOException
import okio.Source
import okio.Timeout
import platform.posix.errno

// TODO: add Cursor implementation
class FileDescriptorSource(val fd: Int) : Source {
  private var closed = false
  private var exhausted = false

  override fun read(
      sink: Buffer,
      byteCount: Long,
  ): Long {
    require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
    check(!closed) { "closed" }
    if (exhausted) {
      return -1L
    }

    return memScoped {
      val bufSize = 8192
      val buf = allocArray<ByteVar>(bufSize)

      var read = 0L
      while (read < byteCount) {
        val toRead = minOf(bufSize.toLong(), byteCount - read)
        val len: Long = platform.posix.read(fd, buf, toRead.convert()).convert()
        if (len < 0) {
          throw IOException("Cannot read $fd (errno = $errno)")
        }
        if (len == 0L) {
          exhausted = true
          return@memScoped read
        }

        read += len
        sink.write(buf.readBytes(len.convert()))

        if (len < toRead) {
          // come back later
          return@memScoped read
        }
      }
      read
    }
  }

  override fun timeout(): Timeout = Timeout.NONE

  override fun close() {
    if (closed) return
    closed = true
    platform.posix.close(fd)
  }
}
