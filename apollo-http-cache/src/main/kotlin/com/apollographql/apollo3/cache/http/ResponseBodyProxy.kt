package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.api.cache.http.HttpCacheRecordEditor
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.internal.Utils.__checkNotNull
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class ResponseBodyProxy(cacheRecordEditor: HttpCacheRecordEditor, sourceResponse: Response,
                                 logger: ApolloLogger) : ResponseBody() {
  private val contentType: String?
  private val contentLength: String?
  private val responseBodySource: BufferedSource
  override fun contentType(): MediaType? {
    return if (contentType != null) MediaType.parse(contentType) else null
  }

  override fun contentLength(): Long {
    return try {
      contentLength?.toLong() ?: -1
    } catch (e: NumberFormatException) {
      -1
    }
  }

  override fun source(): BufferedSource {
    return responseBodySource
  }

  private class ProxySource internal constructor(private val cacheRecordEditor: HttpCacheRecordEditor, private val responseBodySource: BufferedSource, private val logger: ApolloLogger) : Source {
    private val responseBodyCacheSink: ResponseBodyCacheSink
    private var closed = false
    @Throws(IOException::class)
    override fun read(sink: Buffer, byteCount: Long): Long {
      val bytesRead: Long
      try {
        bytesRead = responseBodySource.read(sink, byteCount)
      } catch (e: IOException) {
        if (!closed) {
          // Failed to write a complete cache response.
          closed = true
          abortCacheQuietly()
        }
        throw e
      }
      if (bytesRead == -1L) {
        if (!closed) {
          // The cache response is complete!
          closed = true
          commitCache()
        }
        return -1
      }
      responseBodyCacheSink.copyFrom(sink, sink.size - bytesRead, bytesRead)
      return bytesRead
    }

    override fun timeout(): Timeout {
      return responseBodySource.timeout()
    }

    override fun close() {
      if (closed) return
      closed = true
      if (Utils.discard(this, 100, TimeUnit.MILLISECONDS)) {
        commitCache()
      } else {
        abortCacheQuietly()
      }
    }

    private fun commitCache() {
      Utils.closeQuietly(responseBodySource)
      try {
        responseBodyCacheSink.close()
        cacheRecordEditor.commit()
      } catch (e: Exception) {
        Utils.closeQuietly(responseBodyCacheSink)
        abortCacheQuietly()
        logger.e(e, "Failed to commit cache changes")
      }
    }

    fun abortCacheQuietly() {
      Utils.closeQuietly(responseBodySource)
      Utils.closeQuietly(responseBodyCacheSink)
      try {
        cacheRecordEditor.abort()
      } catch (e: Exception) {
        logger.w(e, "Failed to abort cache edit")
      }
    }

    init {
      responseBodyCacheSink = object : ResponseBodyCacheSink(cacheRecordEditor.bodySink().buffer()) {
        public override fun onException(e: Exception?) {
          abortCacheQuietly()
          logger.w(e, "Operation failed")
        }
      }
    }
  }

  init {
    __checkNotNull(cacheRecordEditor, "cacheRecordEditor == null")
    __checkNotNull(sourceResponse, "sourceResponse == null")
    __checkNotNull(logger, "logger == null")
    contentType = sourceResponse.header("Content-Type")
    contentLength = sourceResponse.header("Content-Length")
    responseBodySource = ProxySource(cacheRecordEditor, sourceResponse.body()!!.source(), logger).buffer()
  }
}