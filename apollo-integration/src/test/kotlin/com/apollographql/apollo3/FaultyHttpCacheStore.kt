package com.apollographql.apollo

import com.apollographql.apollo.api.cache.http.HttpCacheRecord
import com.apollographql.apollo.api.cache.http.HttpCacheRecordEditor
import com.apollographql.apollo.api.cache.http.HttpCacheStore
import com.apollographql.apollo.cache.http.internal.DiskLruCache
import com.apollographql.apollo.cache.http.internal.FileSystem
import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout
import java.io.File
import java.io.IOException

class FaultyHttpCacheStore(fileSystem: FileSystem?) : HttpCacheStore {
  private val cache: DiskLruCache
  val faultySource = FaultySource()
  val faultySink = FaultySink()
  var failStrategy: FailStrategy? = null

  @Throws(IOException::class)
  override fun cacheRecord(cacheKey: String): HttpCacheRecord? {
    val snapshot = cache[cacheKey] ?: return null
    return object : HttpCacheRecord {
      override fun headerSource(): Source {
        return if (failStrategy == FailStrategy.FAIL_HEADER_READ) {
          faultySource
        } else {
          snapshot.getSource(ENTRY_HEADERS)
        }
      }

      override fun bodySource(): Source {
        return if (failStrategy == FailStrategy.FAIL_BODY_READ) {
          faultySource
        } else {
          snapshot.getSource(ENTRY_BODY)
        }
      }

      override fun close() {
        snapshot.close()
      }
    }
  }

  @Throws(IOException::class)
  override fun cacheRecordEditor(cacheKey: String): HttpCacheRecordEditor? {
    val editor = cache.edit(cacheKey) ?: return null
    return object : HttpCacheRecordEditor {
      override fun headerSink(): Sink {
        return if (failStrategy == FailStrategy.FAIL_HEADER_WRITE) {
          faultySink
        } else {
          editor.newSink(ENTRY_HEADERS)
        }
      }

      override fun bodySink(): Sink {
        return if (failStrategy == FailStrategy.FAIL_BODY_WRITE) {
          faultySink
        } else {
          editor.newSink(ENTRY_BODY)
        }
      }

      @Throws(IOException::class)
      override fun abort() {
        editor.abort()
      }

      @Throws(IOException::class)
      override fun commit() {
        editor.commit()
      }
    }
  }

  @Throws(IOException::class)
  override fun delete() {
    cache.delete()
  }

  @Throws(IOException::class)
  override fun remove(cacheKey: String) {
    cache.remove(cacheKey)
  }

  fun failStrategy(failStrategy: FailStrategy?) {
    this.failStrategy = failStrategy
  }

  enum class FailStrategy {
    FAIL_HEADER_READ, FAIL_BODY_READ, FAIL_HEADER_WRITE, FAIL_BODY_WRITE
  }

  class FaultySource : Source {
    @Throws(IOException::class)
    override fun read(sink: Buffer, byteCount: Long): Long {
      throw IOException("failed to read")
    }

    override fun timeout(): Timeout {
      return Timeout()
    }

    @Throws(IOException::class)
    override fun close() {
    }
  }

  class FaultySink : Sink {
    @Throws(IOException::class)
    override fun write(source: Buffer, byteCount: Long) {
      throw IOException("failed to write")
    }

    @Throws(IOException::class)
    override fun flush() {
    }

    override fun timeout(): Timeout {
      return Timeout()
    }

    @Throws(IOException::class)
    override fun close() {
    }
  }

  companion object {
    private const val VERSION = 99991
    private const val ENTRY_HEADERS = 0
    private const val ENTRY_BODY = 1
    private const val ENTRY_COUNT = 2
  }

  init {
    cache = DiskLruCache.create(fileSystem, File("/cache/"), VERSION, ENTRY_COUNT, Int.MAX_VALUE.toLong())
  }
}