package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.api.Logger
import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.cache.http.HttpCacheRecord
import com.apollographql.apollo3.api.cache.http.HttpCacheRecordEditor
import com.apollographql.apollo3.api.cache.http.HttpCacheStore
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.internal.Utils.__checkNotNull
import okhttp3.Interceptor
import okhttp3.Response
import okio.ForwardingSource
import okio.Sink
import okio.Source
import java.io.IOException

class ApolloHttpCache @JvmOverloads constructor(cacheStore: HttpCacheStore, logger: Logger? = null) : HttpCache {
  private val cacheStore: HttpCacheStore
  private val logger: ApolloLogger
  override fun clear() {
    try {
      cacheStore.delete()
    } catch (e: IOException) {
      logger.e(e, "Failed to clear http cache")
    }
  }

  @Throws(IOException::class)
  override fun remove(cacheKey: String) {
    cacheStore.remove(cacheKey)
  }

  override fun removeQuietly(cacheKey: String) {
    try {
      remove(cacheKey)
    } catch (ignore: Exception) {
      logger.w(ignore, "Failed to remove cached record for key: %s", cacheKey)
    }
  }

  override fun read(cacheKey: String): Response? {
    return read(cacheKey, false)
  }

  override fun read(cacheKey: String, expireAfterRead: Boolean): Response? {
    var responseCacheRecord: HttpCacheRecord? = null
    return try {
      responseCacheRecord = cacheStore.cacheRecord(cacheKey)
      if (responseCacheRecord == null) {
        return null
      }
      val cacheRecord: HttpCacheRecord = responseCacheRecord
      val cacheResponseSource: Source = object : ForwardingSource(responseCacheRecord.bodySource()) {
        @Throws(IOException::class)
        override fun close() {
          super.close()
          closeQuietly(cacheRecord)
          if (expireAfterRead) {
            removeQuietly(cacheKey)
          }
        }
      }
      val response = ResponseHeaderRecord(responseCacheRecord.headerSource()).response()
      val contentType = response.header("Content-Type")
      val contentLength = response.header("Content-Length")
      response.newBuilder()
          .addHeader(HttpCache.FROM_CACHE, "true")
          .body(CacheResponseBody(cacheResponseSource, contentType, contentLength))
          .build()
    } catch (e: Exception) {
      closeQuietly(responseCacheRecord)
      logger.e(e, "Failed to read http cache entry for key: %s", cacheKey)
      null
    }
  }

  override fun interceptor(): Interceptor {
    return HttpCacheInterceptor(this, logger)
  }

  fun cacheProxy(response: Response, cacheKey: String): Response {
    if (Utils.skipStoreResponse(response.request())) {
      return response
    }
    var cacheRecordEditor: HttpCacheRecordEditor? = null
    try {
      cacheRecordEditor = cacheStore.cacheRecordEditor(cacheKey)
      if (cacheRecordEditor != null) {
        val headerSink = cacheRecordEditor.headerSink()
        try {
          ResponseHeaderRecord(response).writeTo(headerSink)
        } finally {
          closeQuietly(headerSink)
        }
        return response.newBuilder()
            .body(ResponseBodyProxy(cacheRecordEditor, response, logger))
            .build()
      }
    } catch (e: Exception) {
      abortQuietly(cacheRecordEditor)
      logger.e(e, "Failed to proxy http response for key: %s", cacheKey)
    }
    return response
  }

  fun write(response: Response, cacheKey: String) {
    var cacheRecordEditor: HttpCacheRecordEditor? = null
    try {
      cacheRecordEditor = cacheStore.cacheRecordEditor(cacheKey)
      if (cacheRecordEditor != null) {
        val headerSink = cacheRecordEditor.headerSink()
        try {
          ResponseHeaderRecord(response).writeTo(headerSink)
        } finally {
          closeQuietly(headerSink)
        }
        val bodySink = cacheRecordEditor.bodySink()
        try {
          Utils.copyResponseBody(response, bodySink)
        } finally {
          closeQuietly(bodySink)
        }
        cacheRecordEditor.commit()
      }
    } catch (e: Exception) {
      abortQuietly(cacheRecordEditor)
      logger.e(e, "Failed to cache http response for key: %s", cacheKey)
    }
  }

  fun closeQuietly(cacheRecord: HttpCacheRecord?) {
    try {
      cacheRecord?.close()
    } catch (ignore: Exception) {
      logger.w(ignore, "Failed to close cache record")
    }
  }

  private fun abortQuietly(cacheRecordEditor: HttpCacheRecordEditor?) {
    try {
      cacheRecordEditor?.abort()
    } catch (ignore: Exception) {
      logger.w(ignore, "Failed to abort cache record edit")
    }
  }

  private fun closeQuietly(sink: Sink) {
    try {
      sink.close()
    } catch (ignore: Exception) {
      logger.w(ignore, "Failed to close sink")
    }
  }

  init {
    this.cacheStore = __checkNotNull(cacheStore, "cacheStore == null")
    this.logger = ApolloLogger(logger)
  }
}