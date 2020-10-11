package com.apollographql.apollo

import com.apollographql.apollo.cache.http.internal.FileSystem
import okio.Sink
import okio.Source
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

internal class NoFileSystem : FileSystem {
  @Throws(FileNotFoundException::class)
  override fun source(file: File): Source {
    throw UnsupportedOperationException()
  }

  @Throws(FileNotFoundException::class)
  override fun sink(file: File): Sink {
    throw UnsupportedOperationException()
  }

  @Throws(FileNotFoundException::class)
  override fun appendingSink(file: File): Sink {
    throw UnsupportedOperationException()
  }

  @Throws(IOException::class)
  override fun delete(file: File) {
    throw UnsupportedOperationException()
  }

  override fun exists(file: File): Boolean {
    throw UnsupportedOperationException()
  }

  override fun size(file: File): Long {
    throw UnsupportedOperationException()
  }

  @Throws(IOException::class)
  override fun rename(from: File, to: File) {
    throw UnsupportedOperationException()
  }

  @Throws(IOException::class)
  override fun deleteContents(directory: File) {
    throw UnsupportedOperationException()
  }
}