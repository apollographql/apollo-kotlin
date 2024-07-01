package com.apollographql.apollo.cache.http

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.cache.http.internal.DiskLruCache
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.FileSystem
import okio.Sink
import okio.Source
import okio.Timeout
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
   * Store the [response] with the given [cacheKey] into the cache.
   * A new [HttpResponse] is returned whose body, when read, will write the contents to the cache.
   * The response's body is not consumed nor closed.
   */
  override fun write(response: HttpResponse, cacheKey: String): HttpResponse {
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
        response.body?.let { body(ProxySource(it, bodySink, editor).buffer()) }
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
  override fun remove(cacheKey: String) {
    // is `read` ok here?
    cacheLock.read {
      cache.remove(cacheKey)
    }
  }

  /**
   * A [Source] that writes to the given cache sink as it is read.
   *
   * It commits all successful reads, even if they do not read until EOF. This is so that we can cache Json with extra trailing whitespace.
   * If an error happens when reading the original source or writing to the cache sink, the edit is aborted.
   * The commit or abort is done on [close].
   */
  private class ProxySource(
      private val originalSource: Source,
      private val sink: Sink,
      private val cacheEditor: DiskLruCache.Editor,
  ) : Source {

    private val buffer = Buffer()
    private var hasClosedAndCommitted: Boolean = false
    private var hasReadError: Boolean = false

    override fun read(sink: Buffer, byteCount: Long): Long {
      val read = try {
        originalSource.read(buffer, byteCount)
      } catch (e: Exception) {
        hasReadError = true
        throw e
      }

      if (read == -1L) {
        // We're at EOF
        return -1L
      }
      try {
        buffer.peek().readAll(this.sink)
      } catch (e: Exception) {
        hasReadError = true
      }
      try {
        sink.writeAll(buffer)
      } catch (e: Exception) {
        hasReadError = true
        throw e
      }
      return read
    }

    override fun close() {
      if (!hasClosedAndCommitted) {
        try {
          sink.close()
          if (hasReadError) {
            cacheEditor.abort()
          } else {
            cacheEditor.commit()
          }
        } catch (e: Exception) {
          // Silently ignore cache write errors
        } finally {
          hasClosedAndCommitted = true
        }
        originalSource.close()
      }
    }

    override fun timeout(): Timeout = originalSource.timeout()
  }


  companion object {
    private const val VERSION = 99991
    private const val ENTRY_HEADERS = 0
    private const val ENTRY_BODY = 1
    private const val ENTRY_COUNT = 2
  }
}
