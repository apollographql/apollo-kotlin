/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apollographql.apollo3.cache.http.internal

import com.apollographql.apollo3.cache.http.FileSystem
import okio.Buffer
import okio.ForwardingSink
import okio.Sink
import okio.Source
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Copied from OkHttp 3.14.2:
 * ttps://github.com/square/okhttp/blob/b8b6ee831c65208940c741f8e091ff02425566d5/okhttp-tests
 * /src/test/java/okhttp3/internal/io/FaultyFileSystem.java
 */
class FaultyFileSystem(private val delegate: FileSystem) : FileSystem {
  private val writeFaults: MutableSet<File?> = LinkedHashSet()
  private val deleteFaults: MutableSet<File?> = LinkedHashSet()
  private val renameFaults: MutableSet<File> = LinkedHashSet()
  fun setFaultyWrite(file: File?, faulty: Boolean) {
    if (faulty) {
      writeFaults.add(file)
    } else {
      writeFaults.remove(file)
    }
  }

  fun setFaultyDelete(file: File?, faulty: Boolean) {
    if (faulty) {
      deleteFaults.add(file)
    } else {
      deleteFaults.remove(file)
    }
  }

  fun setFaultyRename(file: File, faulty: Boolean) {
    if (faulty) {
      renameFaults.add(file)
    } else {
      renameFaults.remove(file)
    }
  }

  @Throws(FileNotFoundException::class)
  override fun source(file: File): Source {
    return delegate.source(file)
  }

  @Throws(FileNotFoundException::class)
  override fun sink(file: File): Sink {
    return FaultySink(delegate.sink(file), file)
  }

  @Throws(FileNotFoundException::class)
  override fun appendingSink(file: File): Sink {
    return FaultySink(delegate.appendingSink(file), file)
  }

  @Throws(IOException::class)
  override fun delete(file: File) {
    if (deleteFaults.contains(file)) throw IOException("boom!")
    delegate.delete(file)
  }

  override fun exists(file: File): Boolean {
    return delegate.exists(file)
  }

  override fun size(file: File): Long {
    return delegate.size(file)
  }

  @Throws(IOException::class)
  override fun rename(from: File, to: File) {
    if (renameFaults.contains(from) || renameFaults.contains(to)) throw IOException("boom!")
    delegate.rename(from, to)
  }

  @Throws(IOException::class)
  override fun deleteContents(directory: File) {
    if (deleteFaults.contains(directory)) throw IOException("boom!")
    delegate.deleteContents(directory)
  }

  private inner class FaultySink internal constructor(delegate: Sink?, private val file: File) : ForwardingSink(delegate!!) {
    @Throws(IOException::class)
    override fun write(source: Buffer, byteCount: Long) {
      if (writeFaults.contains(file)) throw IOException("boom!")
      super.write(source, byteCount)
    }
  }
}
