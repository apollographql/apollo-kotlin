package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.cache.http.internal.DiskLruCache
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Sink
import okio.Source
import okio.buffer
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class DiskLruHttpCache(private val fileSystem: FileSystem, private val directory: File, private val maxSize: Long) : ApolloHttpCache {
  private var cache = createDiskLruCache()
  private val cacheLock = ReentrantReadWriteLock()
  private val adapter = Moshi.Builder().build().adapter(Any::class.java)

  private fun createDiskLruCache(): DiskLruCache {
    return DiskLruCache.create(fileSystem, directory, VERSION, ENTRY_COUNT, maxSize)
  }

  @Suppress("UNCHECKED_CAST")
  override fun read(cacheKey: String): HttpResponse {
    val snapshot = cacheLock.read {
      cache[cacheKey]
    } ?: error("HTTP cache: no snapshot")

    val source = snapshot.getSource(ENTRY_HEADERS)
    val map = source.buffer().use {
      adapter.fromJson(it)
    } as? Map<String, Any> ?: error("HTTP cache: no map")

    val headers = (map["headers"] as? List<Map<String, String>>)?.map {
      val entry = it.entries.single()
      HttpHeader(entry.key, entry.value)
    }

    return HttpResponse.Builder(statusCode = (map["statusCode"] as? String)?.toInt() ?: error("HTTP cache: no statusCode"))
        .body(snapshot.getSource(ENTRY_BODY).buffer())
        .addHeaders(headers ?: error("HTTP cache: no headers"))
        .build()
  }

  /**
   * This is not actually called by the current version of the Interceptor, but is kept for backward binary compatibility.
   */
  @Deprecated("Kept for backward binary compatibility")
  override fun write(response: HttpResponse, cacheKey: String) {
    val editor = cacheLock.read {
      cache.edit(cacheKey)
    } ?: return

    try {
      editor.newSink(ENTRY_HEADERS).buffer().use {
        val map = mapOf(
            "statusCode" to response.statusCode.toString(),
            "headers" to response.headers.map { httpHeader ->
              // Moshi doesn't serialize Pairs by default (https://github.com/square/moshi/issues/508) so
              // we use a Map with a single entry
              mapOf(httpHeader.name to httpHeader.value)
            },
        )
        adapter.toJson(it, map)
      }
      editor.newSink(ENTRY_BODY).buffer().use {
        val responseBody = response.body
        if (responseBody != null) {
          it.writeAll(responseBody.peek())
        }
      }
      editor.commit()
    } catch (e: Exception) {
      editor.abort()
    }
  }

  /**
   * Store the [response] with the given [cacheKey] into the cache.
   * A new [HttpResponse] is returned whose body, when read, will write the contents to the cache.
   * The response's body is not consumed nor closed.
   */
  override fun writeIncremental(response: HttpResponse, cacheKey: String): HttpResponse {
    val editor = cacheLock.read {
      cache.edit(cacheKey)
    } ?: return response

    try {
      editor.newSink(ENTRY_HEADERS).buffer().use {
        val map = mapOf(
            "statusCode" to response.statusCode.toString(),
            "headers" to response.headers.map { httpHeader ->
              // Moshi doesn't serialize Pairs by default (https://github.com/square/moshi/issues/508) so
              // we use a Map with a single entry
              mapOf(httpHeader.name to httpHeader.value)
            },
        )
        adapter.toJson(it, map)
      }
      val bodySink = editor.newSink(ENTRY_BODY)
      return HttpResponse.Builder(response.statusCode).apply {
        headers(response.headers)
        response.body?.let { body(WriteToCacheSource(it, bodySink, editor).buffer()) }
      }.build()
    } catch (e: Exception) {
      editor.abort()
      return response
    }
  }

  @Throws(IOException::class)
  override fun clearAll() {
    cache = cacheLock.write {
      cache.delete()
      createDiskLruCache()
    }
  }

  @Throws(IOException::class)
  @Deprecated("Use clearAll() instead", ReplaceWith("clearAll"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_1_1)
  fun delete() {
    clearAll()
  }

  @Throws(IOException::class)
  override fun remove(cacheKey: String) {
    // is `read` ok here?
    cacheLock.read {
      cache.remove(cacheKey)
    }
  }

  /**
   * A [BufferedSource] that writes to the given cache sink as it is read.
   */
  private class WriteToCacheSource(
      private val originalBody: Source,
      private val cacheSink: Sink,
      private val cacheEditor: DiskLruCache.Editor,
  ) : Source by originalBody {

    private var hasClosedAndCommitted: Boolean = false

    override fun read(sink: Buffer, byteCount: Long): Long {
      val buffer = Buffer()
      val read = try {
        originalBody.read(buffer, byteCount)
      } catch (e: Exception) {
        cacheEditor.abort()
        throw e
      }

      if (read == -1L) {
        // We've read fully, commit the cache edit
        closeAndCommitCache()
        return -1L
      }
      buffer.peek().readAll(cacheSink)
      sink.writeAll(buffer)
      return read
    }

    override fun close() {
      closeAndCommitCache()
      originalBody.close()
    }

    private fun closeAndCommitCache() {
      if (!hasClosedAndCommitted) {
        cacheSink.close()
        cacheEditor.commit()
        hasClosedAndCommitted = true
      }
    }
  }


  companion object {
    private const val VERSION = 99991
    private const val ENTRY_HEADERS = 0
    private const val ENTRY_BODY = 1
    private const val ENTRY_COUNT = 2
  }
}
