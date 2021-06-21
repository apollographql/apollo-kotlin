package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.api.cache.http.HttpCacheRecord
import com.apollographql.apollo3.api.cache.http.HttpCacheRecordEditor
import com.apollographql.apollo3.api.cache.http.HttpCacheStore
import com.apollographql.apollo3.cache.http.internal.DiskLruCache
import com.apollographql.apollo3.cache.http.internal.FileSystem
import okio.Sink
import okio.Source
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class DiskLruHttpCacheStore(private val fileSystem: FileSystem, private val directory: File, private val maxSize: Long) : HttpCacheStore {
  private var cache = createDiskLruCache()
  private val cacheLock = ReentrantReadWriteLock()

  constructor(directory: File, maxSize: Long) : this(FileSystem.SYSTEM, directory, maxSize) {}

  private fun createDiskLruCache(): DiskLruCache {
    return DiskLruCache.create(fileSystem, directory, VERSION, ENTRY_COUNT, maxSize)
  }

  @Throws(IOException::class)
  override fun cacheRecord(cacheKey: String): HttpCacheRecord? {
    val snapshot = cacheLock.read {
      cache[cacheKey]
    }
    return if (snapshot == null) {
      null
    } else object : HttpCacheRecord {
      override fun headerSource(): Source {
        return snapshot.getSource(ENTRY_HEADERS)
      }

      override fun bodySource(): Source {
        return snapshot.getSource(ENTRY_BODY)
      }

      override fun close() {
        snapshot.close()
      }
    }
  }

  @Throws(IOException::class)
  override fun cacheRecordEditor(cacheKey: String): HttpCacheRecordEditor? {
    val editor = cacheLock.read {
      cache.edit(cacheKey)
    }

    return if (editor == null) {
      null
    } else object : HttpCacheRecordEditor {
      override fun headerSink(): Sink {
        return editor.newSink(ENTRY_HEADERS)
      }

      override fun bodySink(): Sink {
        return editor.newSink(ENTRY_BODY)
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
    cache = cacheLock.write {
      cache.delete()
      createDiskLruCache()
    }
  }

  @Throws(IOException::class)
  override fun remove(cacheKey: String) {
    // is `read` ok here?
    cacheLock.read {
      cache.remove(cacheKey)
    }
  }

  companion object {
    private const val VERSION = 99991
    private const val ENTRY_HEADERS = 0
    private const val ENTRY_BODY = 1
    private const val ENTRY_COUNT = 2
  }
}